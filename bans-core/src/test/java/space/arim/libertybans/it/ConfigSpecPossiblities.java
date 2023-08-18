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

import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

class ConfigSpecPossiblities {

	private final AnnotatedElement element;

	ConfigSpecPossiblities(AnnotatedElement element) {
		this.element = Objects.requireNonNull(element);
	}

	private Stream<ConfigSpec> getAllPossible(InstanceType instanceType, boolean pluginMessaging, long time) {
		Set<ConfigSpec> possibilities = new HashSet<>();
		for (Vendor vendor : Vendor.values()) {
			for (AddressStrictness addressStrictness : AddressStrictness.values()) {
				for (ServerType serverType : ServerType.values()) {
					possibilities.add(new ConfigSpec(
							vendor, addressStrictness, serverType, instanceType, pluginMessaging, time
					));
				}
			}
		}
		return possibilities.stream();
	}

	private ConfigConstraints getConstraints(PlatformSpecs platformSpecs) {
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
		PlatformSpecs.ServerTypes serverTypeConstraint;
		if (platformSpecs == null) {
			serverTypes = Set.of(ServerType.ONLINE);
		} else if ((serverTypeConstraint = platformSpecs.serverTypes()).all()) {
			serverTypes = Set.of(ServerType.values());
		} else {
			serverTypes = Set.of(serverTypeConstraint.value());
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

	private record ConfigConstraints(Set<Vendor> vendors, Set<AddressStrictness> strictnesses,
									 Set<ServerType> serverTypes) {

		boolean allows(ConfigSpec configSpec) {
				return vendors.contains(configSpec.vendor())
						&& strictnesses.contains(configSpec.addressStrictness())
						&& serverTypes.contains(configSpec.serverType());
		}

	}

	Stream<ConfigSpec> getAll() {
		long time;
		SetTime setTime = element.getAnnotation(SetTime.class);
		if (setTime == null) {
			time = SetTime.DEFAULT_TIME;
		} else {
			time = setTime.unixTime();
		}
		InstanceType instanceType;
		boolean pluginMessaging;
		PlatformSpecs platformSpecs = element.getAnnotation(PlatformSpecs.class);
		if (platformSpecs == null) {
			// Make sure these defaults match those in PlatformSpecs
			instanceType = InstanceType.PROXY;
			pluginMessaging = false;
		} else {
			instanceType = platformSpecs.instanceType();
			pluginMessaging = platformSpecs.pluginMessaging();
		}
		Stream<ConfigSpec> configurations = getAllPossible(instanceType, pluginMessaging, time);
		ConfigConstraints constraints = getConstraints(platformSpecs);
		return configurations.filter(constraints::allows);
	}

}
