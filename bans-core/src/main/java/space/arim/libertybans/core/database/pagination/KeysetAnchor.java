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

import space.arim.libertybans.core.commands.CommandPackage;

import java.util.Optional;

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

    public String chatCode(BorderValueHandle<F> borderValueHandle) {
        StringBuilder builder = new StringBuilder();
        builder.append("k:");
        builder.append(page);
        builder.append(';');
        builder.append(fromForwardScroll ? 1 : 0);
        for (String borderValuePart : borderValueHandle.chatCode(borderValue)) {
            builder.append(';');
            builder.append(borderValuePart);
        }
        return builder.toString();
    }

    public record Build<F>(BorderValueHandle<F> borderValueHandle) {

        Optional<KeysetAnchor<F>> fromCode(String input) {
            if (input.startsWith("k:")) {
                String[] split = input.substring(2).split(";");
                if (split.length >= 2) {
                    int page;
                    try {
                        page = Integer.parseInt(split[0]);
                    } catch (NumberFormatException ignored) {
                        return Optional.empty();
                    }
                    boolean fromForwardScroll = split[1].equals("1");
                    return borderValueHandle.fromCode(2, split).map(
                            (borderValue) -> new KeysetAnchor<>(page, borderValue, fromForwardScroll)
                    );
                }
            }
            return Optional.empty();
        }

        public KeysetAnchor<F> fromCommand(CommandPackage command) {
            if (command.hasNext()) {
                String pageArg = command.next();
                var fromCode = fromCode(pageArg).orElse(null);
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
