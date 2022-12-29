/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.selector;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.SelectionByApplicability;
import space.arim.libertybans.api.select.SelectionByApplicabilityBuilder;

import java.util.Objects;
import java.util.UUID;

public final class SelectionByApplicabilityBuilderImpl
		extends SelectionBuilderBaseImpl<SelectionByApplicabilityBuilder, SelectionByApplicability>
		implements SelectionByApplicabilityBuilder {

	private final SelectorImpl selector;
	private final UUID uuid;
	private final NetworkAddress address;
	private AddressStrictness strictness;
	private final AddressStrictness defaultStrictness;

	SelectionByApplicabilityBuilderImpl(SelectorImpl selector,
										UUID uuid, NetworkAddress address, AddressStrictness defaultStrictness) {
		this.selector = selector;
		this.uuid = uuid;
		this.address = address;
		this.defaultStrictness = defaultStrictness;
		strictness = defaultStrictness;
	}

	@Override
	public SelectionByApplicabilityBuilder addressStrictness(AddressStrictness addressStrictness) {
		strictness = Objects.requireNonNull(addressStrictness, "addressStrictness");
		return this;
	}

	@Override
	public SelectionByApplicabilityBuilder defaultAddressStrictness() {
		return addressStrictness(defaultStrictness);
	}

	@Override
	SelectionByApplicabilityBuilder yieldSelf() {
		return this;
	}

	@Override
	SelectionByApplicability buildWith(SelectionBaseImpl.Details details) {
		return new SelectionByApplicabilityImpl(
				details, selector.resources(), uuid, address, strictness
		);
	}

	// Overrides to enable internal access

	@Override
	public SelectionByApplicabilityBuilderImpl type(PunishmentType type) {
		return (SelectionByApplicabilityBuilderImpl) super.type(type);
	}

	@Override
	public SelectionByApplicabilityImpl build() {
		return (SelectionByApplicabilityImpl) super.build();
	}

}
