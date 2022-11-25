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

package space.arim.libertybans.core.punish;

import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

import java.util.List;

public final class MiscUtil {
	
	private static final List<PunishmentType> PUNISHMENT_TYPES = List.of(PunishmentType.values());
	
	private static final PunishmentType[] PUNISHMENT_TYPES_EXCLUDING_KICK = PUNISHMENT_TYPES.stream()
			.filter((type) -> type != PunishmentType.KICK).toArray(PunishmentType[]::new);
	
	private MiscUtil() {}
	
	/**
	 * Gets an immutable list of PunishmentTypes based on {@link PunishmentType#values()}
	 * 
	 * @return an immutable list of {@link PunishmentType#values()}
	 */
	public static List<PunishmentType> punishmentTypes() {
		return PUNISHMENT_TYPES;
	}
	
	/**
	 * Gets a cached PunishmentType array, excluding {@code KICK} based on {@link PunishmentType#values()}. <br>
	 * <b>Do not mutate the result</b>
	 * 
	 * @return the cached result of {@link PunishmentType#values()} excluding {@code KICK}
	 */
	public static PunishmentType[] punishmentTypesExcludingKick() {
		return PUNISHMENT_TYPES_EXCLUDING_KICK;
	}

	/**
	 * Ensures that the given victim, if it is a composite victim, uses neither {@link CompositeVictim#WILDCARD_UUID}
	 * or {@link CompositeVictim#WILDCARD_ADDRESS}
	 *
	 * @param victim the victim to check
	 * @throws IllegalArgumentException if the victim is composite and uses wildcards
	 */
	static void checkNoCompositeVictimWildcards(Victim victim) {
		if (victim instanceof CompositeVictim compositeVictim) {
			if (compositeVictim.getUUID().equals(CompositeVictim.WILDCARD_UUID)) {
				throw new IllegalArgumentException("Punishments cannot be made with CompositeVictim.WILDCARD_UUID");
			}
			if (compositeVictim.getAddress().equals(CompositeVictim.WILDCARD_ADDRESS)) {
				throw new IllegalArgumentException("Punishments cannot be made with CompositeVictim.WILDCARD_ADDRESS");
			}
		}
	}

	public static RuntimeException unknownVictimType(Victim.VictimType victimType) {
		return new IllegalStateException("Unknown victim type " + victimType);
	}

}
