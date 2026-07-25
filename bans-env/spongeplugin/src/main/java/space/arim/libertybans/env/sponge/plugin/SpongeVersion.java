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

package space.arim.libertybans.env.sponge.plugin;

import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Platform;

import java.util.OptionalInt;

public enum SpongeVersion {
    /*

    Sponge API support matrix as of July 2026

    | API | Minecraft | Status       |
    | --- | --------- | ------------ |
    | 8   | 1.16.5    | EOL          |
    | 9   | 1.18.2    | EOL          |
    | 10  | 1.19.4    | EOL          |
    | 11  | 1.20.6    | EOL          |
    | 12  | 1.21.1    | LTS          |
    | 13  | 1.21.3    | EOL          |
    | 14  | 1.21.4    | EOL          |
    | 15  | 1.21.5    | EOL          |
    | 16  | 1.21.8    | EOL          |
    | 17  | 1.21.10   | Sunsetting   |
    | 18  | 1.21.11   | Supported    |
    | 19  | 26.1      | Experimental |
    | 20  | 26.2      | Experimental |

    Data versions sourced from https://minecraft.wiki/w/Data_version
     */
    API_8(2586), // 1.16.5
    API_9(2975), // 1.18.2
    API_12(3955), // 1.21.1
    API_13(4082), // 1.21.3
    API_15(4325), // 1.21.5
    API_17(4556), // 1.21.10
    API_18(4671), // 1.21.11
    ;

    private final int dataVersion;

    SpongeVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    private String display() {
        return name().replace('_', ' ');
    }

    private boolean isAtLeast(SpongeVersion other) {
        return ordinal() >= other.ordinal();
    }

    private static @Nullable SpongeVersion detectVersion(int dataVersion) {
        SpongeVersion lastPassed = null;
        for (SpongeVersion candidate : SpongeVersion.values()) {
            if (candidate.dataVersion == dataVersion) {
                return candidate;
            }
            if (candidate.dataVersion < dataVersion) {
                lastPassed = candidate;
            }
        }
        return lastPassed;
    }

    static final class ComputeFrom {

        private final Logger logger;
        private final Platform platform;

        ComputeFrom(Logger logger, Platform platform) {
            this.logger = logger;
            this.platform = platform;
        }

        private SpongeVersion unsupported(String msg) {
            logger.error(
                    """
                            ERROR
                            
                            Sorry, however your Sponge server is not supported. You may need to upgrade your server or file a
                            request on the issue tracker. https://github.com/A248/LibertyBans/issues
                            
                            
                            Reason: {}""",
                    msg
            );
            throw new UnsupportedOperationException(msg);
        }

        SpongeVersion detectSupportedVersion() {
            OptionalInt optDataVersion = platform.minecraftVersion().dataVersion();
            if (optDataVersion.isEmpty()) {
                return unsupported("Unknown Minecraft data version (cannot detect Sponge API version)");
            }
            int dataVersion = optDataVersion.getAsInt();
            SpongeVersion spongeVersion = SpongeVersion.detectVersion(dataVersion);
            if (spongeVersion == null) {
                return unsupported("Unknown or unsupported Minecraft data version " + dataVersion);
            }
            // The earliest version we support
            SpongeVersion apiFloor = SpongeVersion.API_15;
            if (!spongeVersion.isAtLeast(apiFloor)) {
                return unsupported("Sponge API version must be at least " + apiFloor + '.');
            }
            // The latest version which we do NOT support
            SpongeVersion apiCeiling = SpongeVersion.API_18;
            if (spongeVersion.isAtLeast(apiCeiling)) {
                return unsupported(
                        "Sponge API version " + spongeVersion + " (or greater) is not supported. Versions "
                                + apiCeiling + " and above are not supported."
                );
            }
            return spongeVersion;
        }

        String displayVersion(SpongeVersion spongeVersion) {
            return platform.container(Platform.Component.API).metadata().version()
					+ " (" + spongeVersion.display() + ')';
        }
    }
}