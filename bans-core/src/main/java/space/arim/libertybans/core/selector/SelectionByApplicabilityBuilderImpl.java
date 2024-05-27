/*
 * LibertyBans
 * Copyright © 2024 Anand Beh
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
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.SelectionByApplicability;
import space.arim.libertybans.api.select.SelectionByApplicabilityBuilder;
import space.arim.libertybans.api.select.SelectionPredicate;

import java.util.Objects;
import java.util.UUID;

public final class SelectionByApplicabilityBuilderImpl
		extends SelectionBuilderBaseImpl<SelectionByApplicabilityBuilder, SelectionByApplicability>
		implements SelectionByApplicabilityBuilder {

	private final SelectionResources resources;
	private final UUID uuid;
	private final NetworkAddress address;
	private AddressStrictness strictness;
	private final AddressStrictness defaultStrictness;
	private boolean potentialNewEntrant;

	SelectionByApplicabilityBuilderImpl(SelectionResources resources,
										UUID uuid, NetworkAddress address, AddressStrictness defaultStrictness) {
		this.resources = resources;
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
				details, resources, uuid, address, strictness, potentialNewEntrant
		);
	}

	// Overrides to enable internal access

	@Override
	public SelectionByApplicabilityBuilderImpl type(PunishmentType type) {
		return (SelectionByApplicabilityBuilderImpl) super.type(type);
	}

	@Override
	public SelectionByApplicabilityBuilderImpl scopes(SelectionPredicate<ServerScope> scopes) {
		return (SelectionByApplicabilityBuilderImpl) super.scopes(scopes);
	}

	@Override
	public SelectionByApplicabilityImpl build() {
		return (SelectionByApplicabilityImpl) super.build();
	}

	/**
	 * For internal use, to accomodate a new user whose UUID and IP address combination
	 * has not yet been entered to the database. Sets whether this scenario is potential.
	 *
	 * @param canAssumeUserRecorded if the user is definitely registered, set to true. True by default.
	 * @return this builder
	 */
	public SelectionByApplicabilityBuilderImpl canAssumeUserRecorded(boolean canAssumeUserRecorded) {
		// A user is a potential new entrant if we can't assume they're recorded
		this.potentialNewEntrant = !canAssumeUserRecorded;
		return this;
	}

}
