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

package space.arim.libertybans.env.sponge.plugin;

import java.util.Optional;

public enum SpongeVersion {
    /*

    Sponge API support matrix

    | API | Minecraft | Status       |
    | --- | --------- | ------------ |
    | 8   | 1.16.5    | EOL          |
    | 9   | 1.18.2    | EOL          |
    | 10  | 1.19.4    | EOL          |
    | 11  | 1.20.6    | EOL          |
    | 12  | 1.21.1    | LTS          |
    | 13  | 1.21.3    | Supported    |
    | 14  | 1.21.4    | Experimental |
    | 15  | 1.21.5    | Experimental |

    Data versions sourced from https://minecraft.wiki/w/Data_version
     */
    API_8(2586), // 1.16.5
    API_9(2975), // 1.18.2
    API_12(3955), // 1.21.1
    API_13(4082); // 1.21.3

    private final int dataVersion;

    SpongeVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    public String display() {
        return name().replace('_', ' ');
    }

    public boolean isAtLeast(SpongeVersion other) {
        return ordinal() >= other.ordinal();
    }

    boolean hasSnakeYaml() {
        return this == API_8 || this == API_9;
    }

    public static Optional<SpongeVersion> detectVersion(int dataVersion) {
        for (SpongeVersion possible : SpongeVersion.values()) {
            if (possible.dataVersion == dataVersion) {
                return Optional.of(possible);
            }
        }
        return Optional.empty();
    }

    public static SpongeVersion latestSupported() {
        var allValues = SpongeVersion.values();
        return allValues[allValues.length - 1];
    }
}