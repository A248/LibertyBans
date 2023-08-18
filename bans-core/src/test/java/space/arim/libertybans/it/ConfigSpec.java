/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.it;

import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.core.uuid.ServerType;

import java.time.Instant;
import java.util.Objects;

public record ConfigSpec(Vendor vendor, AddressStrictness addressStrictness, ServerType serverType,
						 InstanceType instanceType, boolean pluginMessaging, long unixTime) {

	public ConfigSpec {
		Objects.requireNonNull(vendor, "vendor");
		Objects.requireNonNull(addressStrictness, "addressStrictness");
		Objects.requireNonNull(serverType, "serverType");
		Objects.requireNonNull(instanceType, "instanceType");
	}

	public Instant unixTimestamp() {
		return Instant.ofEpochSecond(unixTime);
	}

}
