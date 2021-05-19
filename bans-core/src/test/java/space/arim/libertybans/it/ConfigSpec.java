/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
import space.arim.libertybans.core.selector.AddressStrictness;
import space.arim.libertybans.core.uuid.ServerType;

import java.time.Instant;
import java.util.Objects;

public final class ConfigSpec {
	
	private final Vendor vendor;
	private final AddressStrictness addressStrictness;
	private final ServerType serverType;
	private final long unixTime;
	
	ConfigSpec(Vendor vendor, AddressStrictness addressStrictness, ServerType serverType, long unixTime) {
		this.vendor = Objects.requireNonNull(vendor, "vendor");
		this.addressStrictness = Objects.requireNonNull(addressStrictness, "addressStrictness");
		this.serverType = Objects.requireNonNull(serverType, "serverType");
		this.unixTime = unixTime;
	}
	
	public Vendor vendor() {
		return vendor;
	}
	
	public AddressStrictness addressStrictness() {
		return addressStrictness;
	}

	public ServerType serverType() {
		return serverType;
	}

	public Instant unixTime() {
		return Instant.ofEpochSecond(unixTime);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ConfigSpec that = (ConfigSpec) o;
		return unixTime == that.unixTime && vendor == that.vendor
				&& addressStrictness == that.addressStrictness && serverType == that.serverType;
	}

	@Override
	public int hashCode() {
		int result = vendor.hashCode();
		result = 31 * result + addressStrictness.hashCode();
		result = 31 * result + serverType.hashCode();
		result = 31 * result + (int) (unixTime ^ (unixTime >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "ConfigSpec{" +
				"vendor=" + vendor +
				", addressStrictness=" + addressStrictness +
				", serverType=" + serverType +
				", unixTime=" + unixTime +
				'}';
	}

}
