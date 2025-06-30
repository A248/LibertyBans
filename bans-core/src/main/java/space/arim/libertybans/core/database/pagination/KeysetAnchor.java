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

package space.arim.libertybans.core.database.pagination;

import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.core.commands.CommandPackage;

import java.time.Instant;

/**
 * An anchor for pagination purposes
 *
 * @param page              the current page
 * @param borderValue       the border value which got us to the current page
 * @param fromForwardScroll whether we got here from forward or backward scrolling
 * @param <F>               the type of the sorted field
 */
public record KeysetAnchor<F>(int page, F borderValue, boolean fromForwardScroll) {

    private static final KeysetAnchor<?> UNSET = new KeysetAnchor<>(1, null, true);

    private static final String CHAT_CODE_PREFIX = "k:";

    /**
     * Gets an unset anchor, that is, one starting on the first page
     *
     * @return an unset anchor
     * @param <F> the type of the sorted field
     */
    @SuppressWarnings("unchecked")
    public static <F> KeysetAnchor<F> unset() {
        return (KeysetAnchor<F>) UNSET;
    }

    String chatCode(BorderValueHandle<F> borderValueHandle) {
        StringBuilder builder = new StringBuilder();
        builder.append(CHAT_CODE_PREFIX);
        builder.append(page);
        builder.append(';');
        builder.append(fromForwardScroll ? 1 : 0);
        borderValueHandle.writeChatCode(borderValue, borderValuePart -> {
            builder.append(';');
            builder.append(borderValuePart);
        });
        return builder.toString();
    }

    public static KeysetAnchor<Instant> instant(CommandPackage command) {
        return new Build<>(new InstantBorderValue(new LongBorderValue())).fromCommand(command);
    }

    public static KeysetAnchor<InstantThenUUID> instantThenUUID(CommandPackage command) {
        return new Build<>(InstantThenUUID.borderValueHandle()).fromCommand(command);
    }

    record Build<F>(BorderValueHandle<F> borderValueHandle) {

        @Nullable
        KeysetAnchor<F> fromCode(String input) {
            if (input.startsWith(CHAT_CODE_PREFIX)) {
                String[] split = input.substring(CHAT_CODE_PREFIX.length()).split(";");
                if (split.length >= 2 + borderValueHandle.len()) {
                    int page;
                    try {
                        page = Integer.parseInt(split[0]);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                    boolean fromForwardScroll = split[1].equals("1");
                    F borderValue = borderValueHandle.readChatCode(new ArrayCursor(split, 2));
                    if (borderValue != null) {
                        return new KeysetAnchor<>(page, borderValue, fromForwardScroll);
                    }
                }
            }
            return null;
        }

        private KeysetAnchor<F> fromCommand(CommandPackage command) {
            if (command.hasNext()) {
                String pageArg = command.next();
                var fromCode = fromCode(pageArg);
                if (fromCode != null) {
                    return fromCode;
                }
                try {
                    int page = Integer.parseInt(pageArg);
                    if (page > 0) {
                        return new KeysetAnchor<>(page, null, true);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return unset();
        }
    }
}
