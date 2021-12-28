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

package space.arim.libertybans.core.commands.extra;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;

import java.util.function.Function;

public final class AsCompositeWildcard implements Function<Victim, CompositeVictim> {

	@Override
	public CompositeVictim apply(Victim victim) {
		Victim.VictimType victimType = victim.getType();
		switch (victimType) {
		case PLAYER:
			return CompositeVictim.of(((PlayerVictim) victim).getUUID(), CompositeVictim.WILDCARD_ADDRESS);
		case ADDRESS:
			return CompositeVictim.of(CompositeVictim.WILDCARD_UUID, ((AddressVictim) victim).getAddress());
		default:
			throw new UnsupportedOperationException("not supported: " + victimType);
		}
	}
}
