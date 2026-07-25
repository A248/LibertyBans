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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.jsonchat.ClickEventInfo;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;
import space.arim.morepaperlib.adventure.ClickEventType;
import space.arim.morepaperlib.adventure.MorePaperLibAdventure;

import java.util.function.Function;

public final class DynamicAdventure5Compat implements Adventure5Compat {

    private final MorePaperLibAdventure morePaperLibAdventure;

    @Inject
    public DynamicAdventure5Compat(MorePaperLibAdventure morePaperLibAdventure) {
        this.morePaperLibAdventure = morePaperLibAdventure;
    }

    @Override
    public @Nullable ComponentBuilder<?, ?> toBuilder(Component component) {
        return morePaperLibAdventure.componentToBuilder(component);
    }

    @Override
    public ClickEvent mapClickEventValue(ClickEvent original, Function<? super String, String> stringMap) {
        ClickEventType clickEventType = morePaperLibAdventure.clickEventType(original.action());
        if (clickEventType == null) {
            return original;
        }
        String oldValue = clickEventValue(original);
        String newValue = stringMap.apply(oldValue);
        if (oldValue.equals(newValue)) {
            return original;
        }
        return morePaperLibAdventure.clickEvent(clickEventType, newValue);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String clickEventValue(ClickEvent clickEvent) {
        return switch (morePaperLibAdventure.adventureVersion()) {
            case VER_5_1, VER_4_26 -> valueOfPayload(clickEvent);
            default -> clickEvent.value();
        };
    }

    private String valueOfPayload(ClickEvent clickEvent) {
        ClickEvent.Payload payload = clickEvent.payload();
        if (payload instanceof ClickEvent.Payload.Text) {
            return ((ClickEvent.Payload.Text) payload).value();
        }
        if (payload instanceof ClickEvent.Payload.Int) {
            return Integer.toString(((ClickEvent.Payload.Int) payload).integer());
        }
        return clickEvent.payload().toString();
    }

    @Override
    public ClickEventInfo.ClickType clickActionToType(ClickEvent.Action action) {
        ClickEventType clickEventType = morePaperLibAdventure.clickEventType(action);
        if (clickEventType == null) {
            throw new UnsupportedOperationException("Unknown action: " + action);
        }
        return switch (clickEventType) {
            case RUN_COMMAND -> ClickEventInfo.ClickType.RUN_COMMAND;
            case SUGGEST_COMMAND -> ClickEventInfo.ClickType.SUGGEST_COMMAND;
            case OPEN_URL -> ClickEventInfo.ClickType.OPEN_URL;
            default -> throw new UnsupportedOperationException("Unknown click event type: " + clickEventType);
        };
    }

    @Override
    public ClickEvent.Action clickTypeToAction(ClickEventInfo.ClickType clickType) {
        ClickEventType clickEventType = switch (clickType) {
            case RUN_COMMAND -> ClickEventType.RUN_COMMAND;
            case SUGGEST_COMMAND -> ClickEventType.SUGGEST_COMMAND;
            case OPEN_URL -> ClickEventType.OPEN_URL;
        };
        return morePaperLibAdventure.clickEventAction(clickEventType);
    }

    @Override
    public Component join(ComponentLike separator, ComponentLike... components) {
        return morePaperLibAdventure.joinComponents(separator, components);
    }
}
