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

package space.arim.libertybans.core.database.sql;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;

import java.util.Objects;
import java.util.UUID;

public final class SerializedVictim implements VictimData {

	private final Victim victim;

	public SerializedVictim(Victim victim) {
		this.victim = Objects.requireNonNull(victim, "victim");
	}

	@Override
	public Victim.VictimType type() {
		return victim.getType();
	}

	@Override
	public UUID uuid() {
		return switch (victim.getType()) {
			case PLAYER -> ((PlayerVictim) victim).getUUID();
			case ADDRESS -> EmptyData.UUID;
			case COMPOSITE -> ((CompositeVictim) victim).getUUID();
		};
	}

	@Override
	public NetworkAddress address() {
		return switch (victim.getType()) {
			case PLAYER -> EmptyData.ADDRESS;
			case ADDRESS -> ((AddressVictim) victim).getAddress();
			case COMPOSITE -> ((CompositeVictim) victim).getAddress();
		};
	}

	@Override
	public String toString() {
		return "SerializedVictim{" +
				"victim=" + victim +
				'}';
	}

}
