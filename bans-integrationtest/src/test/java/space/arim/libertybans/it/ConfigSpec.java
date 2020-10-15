/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.core.selector.AddressStrictness;

public class ConfigSpec {
	
	private final Vendor vendor;
	private final AddressStrictness addressStrictness;
	private final int port;
	private final String database;
	
	ConfigSpec(Vendor vendor, AddressStrictness addressStrictness) {
		this(vendor, addressStrictness, -1, "");
	}
	
	ConfigSpec(Vendor vendor, AddressStrictness addressStrictness, int port, String database) {
		this.vendor = Objects.requireNonNull(vendor, "vendor");
		this.addressStrictness = Objects.requireNonNull(addressStrictness, "addressStrictness");
		this.port = port;
		this.database = database;
	}
	
	public Vendor getVendor() {
		return vendor;
	}
	
	public AddressStrictness getAddressStrictness() {
		return addressStrictness;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getDatabase() {
		return database;
	}
	
	static Set<ConfigSpec> getAll() {
		Set<ConfigSpec> possibilities = new HashSet<>();
		for (Vendor vendor : Vendor.values()) {
			for (AddressStrictness addressStrictness : AddressStrictness.values()) {
				possibilities.add(new ConfigSpec(vendor, addressStrictness));
			}
		}
		return possibilities;
	}
	
	boolean agrees(ConfigConstraints constraints) {
		return containsOrEmpty(constraints.vendor(), vendor)
				&& containsOrEmpty(constraints.addressStrictness(), addressStrictness);
	}
	
	private static <T extends Enum<T>> boolean containsOrEmpty(T[] array, T element) {
		if (array.length == 0) {
			return true;
		}
		for (T e : array) {
			if (e == element) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + addressStrictness.hashCode();
		result = prime * result + vendor.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ConfigSpec)) {
			return false;
		}
		ConfigSpec other = (ConfigSpec) obj;
		return addressStrictness == other.addressStrictness && vendor == other.vendor;
	}

	@Override
	public String toString() {
		return "ConfigSpec [vendor=" + vendor + ", addressStrictness=" + addressStrictness + "]";
	}
	
}
