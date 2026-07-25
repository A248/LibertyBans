/*
 * LibertyBans-bootstrap
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */

package space.arim.libertybans.bootstrap;

import java.util.Objects;

public record PlatformId(String name, String version) {

    public static final PlatformId STUB = new PlatformId("Stub", "0.0");

    public PlatformId {
        name = Objects.requireNonNullElse(name, "unnamed");
        version = Objects.requireNonNullElse(version, "0.0");
    }
}
