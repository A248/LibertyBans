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

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.addon.AddonConfig;

import java.net.URI;

@ConfHeader({
        "Configuration for sending webhooks every time a player is punished.",
        "",
        "Most people will use this addon for Discord webhooks. As a result, the comments guide people through",
        "creating a Discord webhook and configuring it here. However, it is technically possible to send any kind of",
        "webhook to any kind of URL; the free-form json payload enables this possibility."
})
public interface WebhookConfig extends AddonConfig {


    @ConfKey("webhook-url")
    @ConfComments({
            "This is the URL of your webhook. Do not share this with anyone!",
            "visit your Discord server's admin panel, then click on Integrations > New Webhook.",
            "Click 'Copy Webhook URL' and paste it here."
    })
    @ConfDefault.DefaultString("https://mysite.com")
    URI webhookUrl();

    @ConfKey("on-punish")
    @SubSection
    @ConfComments("When a player is punished, this webhook is sent")
    EventPayload onPunish();

    @ConfKey("on-pardon")
    @SubSection
    @ConfComments("When a player is unpunished, this webhook is sent. Note that punishments expiring will NOT trigger this webhook.")
    EventPayload onPardon();

    interface EventPayload {

        @ConfComments("Whether to enable the webhook for this event")
        @ConfDefault.DefaultBoolean(false)
        boolean enable();

        @ConfKey("json-payload")
        @ConfComments({
                "The raw JSON that will be sent to the Discord webhook.",
                "A Discord webhook has many potential fields, so this section lets you add whatever you want.",
                "",
                "Note that punishment-related variables are fully supported here, but color codes are not.",
                "",
                "We HIGHLY recommend using a tool, like Discohook, to create a webhook",
                "Steps:",
                "1. Visit https://discohook.org/",
                "2. Make the webhook look how you want it.",
                "3. Click 'JSON Editor' and copy the text",
                "4. Minify the JSON using https://www.minifyjson.org/ so that it fits on a single line",
                "5. Copy the minified version here.",
                "",
                "If editing the raw JSON, it's recommended to use a JSON minifier/prettifier to help visualize your changes.",
                "For example, https://www.minifyjson.org/ . Use the \"Beautify\" button to see the JSON, then edit it.",
                "Once you're done, use the \"Minify\" button and paste the result back here.",
                "",
                "You can see a full list of options by checking Discord's documentation, or by browsing some examples.",
                "Official reference:",
                "https://discord.com/developers/docs/resources/webhook#execute-webhook-jsonform-params",
                "Examples and guide:",
                "https://gist.github.com/Birdie0/78ee79402a4301b1faf412ab5f1cdcf9#example-for-a-webhook"
        })
        @ConfDefault.DefaultString("{\"content\":\"BAM! %VICTIM% has been punished with %TYPE%.\\n\\nThere's more info below, but you don't have to read it. This punishment will last for %DURATION%\\n_ _\",\"embeds\":[{\"title\":\"Punishment reason\",\"description\":\"%REASON%\",\"color\":5814783,\"fields\":[{\"name\":\"Operator\",\"value\":\"By %OPERATOR%\"},{\"name\":\"Scope\",\"value\":\"%SCOPE%\"}],\"author\":{\"name\":\"LibertyBans\"}}]}")
        String jsonPayload();

    }

}
