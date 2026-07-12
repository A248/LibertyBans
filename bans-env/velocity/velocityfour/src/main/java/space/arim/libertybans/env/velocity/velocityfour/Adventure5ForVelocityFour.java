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

package space.arim.libertybans.env.velocity.velocityfour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NonNull;
import space.arim.api.jsonchat.ClickEventInfo;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;

import java.util.function.Function;

public final class Adventure5ForVelocityFour implements Adventure5Compat {
    @Override
    public @NonNull ComponentBuilder<?, ?> toBuilder(Component component) {
        return component.toBuilder();
    }

    @Override
    public ClickEvent<?> mapClickEventValue(ClickEvent original, Function<? super String, String> stringMap) {
        ClickEventInfo.ClickType clickType = clickTypeOrNull(original.action());
        if (clickType == null) {
            return original;
        }
        String oldValue = clickEventValue(original);
        String newValue = stringMap.apply(oldValue);
        if (oldValue.equals(newValue)) {
            return original;
        }
        return switch (clickType) {
            case OPEN_URL -> ClickEvent.openUrl(newValue);
            case SUGGEST_COMMAND -> ClickEvent.suggestCommand(newValue);
            case RUN_COMMAND -> ClickEvent.runCommand(newValue);
        };
    }

    @Override
    public String clickEventValue(ClickEvent clickEvent) {
        ClickEvent.Payload payload = clickEvent.payload();
        if (payload instanceof ClickEvent.Payload.Text) {
            return ((ClickEvent.Payload.Text) payload).value();
        }
        if (payload instanceof ClickEvent.Payload.Int) {
            return Integer.toString(((ClickEvent.Payload.Int) payload).integer());
        }
        return payload.toString();
    }

    @Override
    public ClickEventInfo.ClickType clickActionToType(ClickEvent.Action action) {
        ClickEventInfo.ClickType clickType = clickTypeOrNull(action);
        if (clickType == null) {
            throw new UnsupportedOperationException("Unknown action: " + action);
        }
        return clickType;
    }

    private ClickEventInfo.@Nullable ClickType clickTypeOrNull(ClickEvent.Action<?> action) {
        if (action.equals(ClickEvent.Action.SUGGEST_COMMAND)) {
            return ClickEventInfo.ClickType.SUGGEST_COMMAND;
        }
        if (action.equals(ClickEvent.Action.RUN_COMMAND)) {
            return ClickEventInfo.ClickType.RUN_COMMAND;
        }
        if (action.equals(ClickEvent.Action.OPEN_URL)) {
            return ClickEventInfo.ClickType.OPEN_URL;
        }
        return null;
    }

    @Override
    public ClickEvent.Action<?> clickTypeToAction(ClickEventInfo.ClickType clickType) {
        return switch (clickType) {
            case SUGGEST_COMMAND -> ClickEvent.Action.SUGGEST_COMMAND;
            case RUN_COMMAND -> ClickEvent.Action.RUN_COMMAND;
            case OPEN_URL -> ClickEvent.Action.OPEN_URL;
        };
    }

    @Override
    public Component join(ComponentLike separator, ComponentLike... components) {
        return Component.join(JoinConfiguration.separator(separator), components);
    }
}
