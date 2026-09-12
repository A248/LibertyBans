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

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlainTextTest {

    @Test
    public void keepsTextAfterAComponentSubstitution() {
        String payload = "{\"content\":\"x%SILENCE%y\"}";

        Component component = Component.text(payload).replaceText((config) -> config
                .matchLiteral("%SILENCE%")
                .replacement(Component.text("[silent]")));

        assertEquals("{\"content\":\"x[silent]y\"}", PostWebhookListener.plainText(component));
    }

    @Test
    public void keepsTextAfterSeveralSubstitutions() {
        String payload = "{\"a\":\"%SILENCE%\",\"b\":\"z\"}";

        Component component = Component.text(payload)
                .replaceText((config) -> config.matchLiteral("%SILENCE%").replacement(Component.text("s")))
                .replaceText((config) -> config.matchLiteral("%HAS_EXPIRED%").replacement(Component.text("e")));

        assertEquals("{\"a\":\"s\",\"b\":\"z\"}", PostWebhookListener.plainText(component));
    }

    @Test
    public void leavesAPayloadWithoutVariablesUnchanged() {
        String payload = "{\"content\":\"plain\"}";

        assertEquals(payload, PostWebhookListener.plainText(Component.text(payload)));
    }
}
