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
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.core.uuid.ServerType;

import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

class ConfigSpecPossiblities {

	private final AnnotatedElement element;

	ConfigSpecPossiblities(AnnotatedElement element) {
		this.element = element;
	}

	private Stream<ConfigSpec> getAllPossible(long time) {
		Set<ConfigSpec> possibilities = new HashSet<>();
		for (Vendor vendor : Vendor.values()) {
			for (AddressStrictness addressStrictness : AddressStrictness.values()) {
				for (ServerType serverType : ServerType.values()) {
					possibilities.add(new ConfigSpec(vendor, addressStrictness, serverType, time));
				}
			}
		}
		return possibilities.stream();
	}

	private ConfigConstraints getConstraints() {
		if (element.getAnnotation(NoDbAccess.class) != null) {
			return new ConfigConstraints(
					Set.of(Vendor.HSQLDB), Set.of(AddressStrictness.NORMAL), Set.of(ServerType.ONLINE));
		}
		Set<AddressStrictness> addressStrictnesses;
		{
			SetAddressStrictness strictnessConstraint = element.getAnnotation(SetAddressStrictness.class);
			if (strictnessConstraint == null) {
				addressStrictnesses = Set.of(AddressStrictness.NORMAL);
			} else if (strictnessConstraint.all()) {
				addressStrictnesses = Set.of(AddressStrictness.values());
			} else {
				addressStrictnesses = Set.of(strictnessConstraint.value());
			}
		}
		Set<ServerType> serverTypes;
		{
			SetServerType serverTypeConstraint = element.getAnnotation(SetServerType.class);
			if (serverTypeConstraint == null) {
				serverTypes = Set.of(ServerType.ONLINE);
			} else if (serverTypeConstraint.all()) {
				serverTypes = Set.of(ServerType.values());
			} else {
				serverTypes = Set.of(serverTypeConstraint.value());
			}
		}
		Set<Vendor> vendors;
		{
			SetVendor vendorConstraint = element.getAnnotation(SetVendor.class);
			if (vendorConstraint == null) {
				vendors = Set.of(Vendor.values());
			} else {
				vendors = Set.of(vendorConstraint.value());
			}
		}
		return new ConfigConstraints(vendors, addressStrictnesses, serverTypes);
	}

	private static class ConfigConstraints {

		private final Set<AddressStrictness> strictnesses;
		private final Set<Vendor> vendors;
		private final Set<ServerType> serverTypes;

		ConfigConstraints(Set<Vendor> vendors, Set<AddressStrictness> strictnesses, Set<ServerType> serverTypes) {

			this.vendors = vendors;
			this.strictnesses = strictnesses;
			this.serverTypes = serverTypes;
		}

		boolean allows(ConfigSpec configSpec) {
			return vendors.contains(configSpec.vendor())
					&& strictnesses.contains(configSpec.addressStrictness())
					&& serverTypes.contains(configSpec.serverType());
		}

	}

	Stream<ConfigSpec> getAll() {
		long defaultTime = SetTime.DEFAULT_TIME;
		if (element == null) {
			return getAllPossible(defaultTime);
		}
		long time = defaultTime;
		SetTime setTime = element.getAnnotation(SetTime.class);
		if (setTime != null) {
			time = setTime.unixTime();
		}
		Stream<ConfigSpec> configurations = getAllPossible(time);
		ConfigConstraints constraints = getConstraints();
		return configurations.filter(constraints::allows);
	}

}
