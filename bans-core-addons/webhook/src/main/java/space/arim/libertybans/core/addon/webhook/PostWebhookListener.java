/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.Operator;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
public final class PostWebhookListener {

    private final WebhookAddon addon;
    private final InternalFormatter formatter;
    private final FuturePoster futurePoster;
    private final HttpClient client = HttpClient.newHttpClient();

    private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

    @Inject
    public PostWebhookListener(WebhookAddon addon, InternalFormatter formatter, FuturePoster futurePoster) {
        this.addon = addon;
        this.formatter = formatter;
        this.futurePoster = futurePoster;
    }

    @ListeningMethod(priority = ListenerPriorities.LOW)
    public void onPunish(PostPunishEvent event) {
        onEvent(event.getPunishment(), event.getTarget().orElse(null), null, WebhookConfig::onPunish);
    }

    @ListeningMethod(priority = ListenerPriorities.LOW)
    public void onPardon(PostPardonEvent event) {
        onEvent(event.getPunishment(), event.getTarget().orElse(null), event.getOperator(), WebhookConfig::onPardon);
    }

    private void onEvent(Punishment punishment, @Nullable String target, @Nullable Operator unOperator,
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
        jsonPayload = jsonPayload.replace("%TARGET%", target == null ? "<none>" : target);
        ComponentText formattable = ComponentText.create(Component.text(jsonPayload));
        CompletableFuture<Component> formatted;
        if (unOperator == null) {
            formatted = formatter.formatWithPunishment(formattable, punishment);
        } else {
            formatted = formatter.formatWithPunishmentAndUnoperator(formattable, punishment, unOperator);
        }
        var future = formatted.thenCompose(component -> {
            String json = ((TextComponent) component).content();
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
}
