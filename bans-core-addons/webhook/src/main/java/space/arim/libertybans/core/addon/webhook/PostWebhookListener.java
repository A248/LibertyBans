/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.addon.webhook;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.event.PostOpNotificationEvent;
import space.arim.libertybans.api.event.PostPardonEvent;
import space.arim.libertybans.api.event.PostPunishEvent;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.service.FuturePoster;
import space.arim.omnibus.events.ListenerPriorities;
import space.arim.omnibus.events.ListeningMethod;
import space.arim.omnibus.util.ThisClass;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
public final class PostWebhookListener {

    private final WebhookAddon addon;
    private final InternalFormatter formatter;
    private final FuturePoster futurePoster;
    private final Adventure5Compat adventure5Compat;
    private final HttpClient client = HttpClient.newHttpClient();

    private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

    @Inject
    public PostWebhookListener(WebhookAddon addon, InternalFormatter formatter, FuturePoster futurePoster,
                               Adventure5Compat adventure5Compat) {
        this.addon = addon;
        this.formatter = formatter;
        this.futurePoster = futurePoster;
        this.adventure5Compat = adventure5Compat;
    }

    @ListeningMethod(priority = ListenerPriorities.LOW)
    public void onPunish(PostPunishEvent event) {
        onEvent(event, null, WebhookConfig::onPunish);
    }

    @ListeningMethod(priority = ListenerPriorities.LOW)
    public void onPardon(PostPardonEvent event) {
        Operator unOperator = event.getOperator();
        Objects.requireNonNull(unOperator, "unOperator"); // bad event impl
        onEvent(event, unOperator, WebhookConfig::onPardon);
    }

    private void onEvent(PostOpNotificationEvent event, @Nullable Operator unOperator,
                         Function<WebhookConfig, WebhookConfig.EventPayload> getEventPayload) {
        WebhookConfig config = addon.config();
        if (!config.enable()) {
            return;
        }
        URI webhookUrl = config.webhookUrl();
        WebhookConfig.EventPayload eventPayload = getEventPayload.apply(config);
        if (!eventPayload.enable()) {
            return;
        }
        String jsonPayload = eventPayload.jsonPayload();
        {
            String target = event.getTarget().orElse(null);
            jsonPayload = jsonPayload.replace("%TARGET%", target == null ? "<none>" : target);
        }
        ComponentText formattable = ComponentText.create(Component.text(jsonPayload), adventure5Compat);
        CompletableFuture<Component> formatted;
        {
            Punishment punishment = event.getPunishment();
            boolean silent = event.isSilent();
            if (unOperator == null) {
                formatted = formatter.formatNotificationIssue(formattable, punishment, silent);
            } else {
                formatted = formatter.formatNotificationRevoke(formattable, punishment, unOperator, silent);
            }
        }
        var future = formatted.thenCompose(component -> {
            String json = plainText(component);
            return postWebhook(webhookUrl, json);
        });
        futurePoster.postFuture(future);
    }

    private CompletableFuture<Void> postWebhook(URI webhookUrl, String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(webhookUrl)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(40L))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
            int statusCode = response.statusCode();
            if (statusCode == 200 || statusCode == 204) {
                logger.debug("Successfully posted webhook");
            } else {
                logger.warn("Received unexpected status code {} from webhook API", statusCode);
            }
        });
    }

    /*
     * Substituting a component-valued variable such as %SILENCE% splits the payload across child
     * components, leaving only the text before the first match on the root. Reading the root's
     * content truncated the payload and sent invalid JSON, so walk the whole tree instead.
     *
     * Adventure's plain-text serializer cannot do this here. Adventure 4 calls it
     * PlainComponentSerializer and Adventure 5 calls it PlainTextComponentSerializer, and both are
     * supported at runtime, so neither can be compiled against.
     */
    static String plainText(Component component) {
        StringBuilder builder = new StringBuilder();
        appendPlainText(component, builder);
        return builder.toString();
    }

    private static void appendPlainText(Component component, StringBuilder builder) {
        if (component instanceof TextComponent textComponent) {
            builder.append(textComponent.content());
        }
        for (Component child : component.children()) {
            appendPlainText(child, builder);
        }
    }
}
