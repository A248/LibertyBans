/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core;

import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;

public final class MiscUtil {
	
	/**
	 * The maximum amount of which can be stored in an INT UNSIGNED data type
	 */
	private static final long INT_UNSIGNED_MAX_VALUE = Integer.MAX_VALUE - Integer.MIN_VALUE;
	
	private static final PunishmentType[] PUNISHMENT_TYPES = PunishmentType.values();
	
	private MiscUtil() {}
	
	/**
	 * Gets a cached PunishmentType array based on {@link PunishmentType#values()}. <br>
	 * <b>DO NOT MUTATE the result</b>
	 * 
	 * @return the cached result of {@link PunishmentType#values()}
	 */
	public static PunishmentType[] punishmentTypes() {
		return PUNISHMENT_TYPES;
	}
	
	/**
	 * Translates a java enum into a MySQL ENUM type, including a NOT NULL constraint
	 * 
	 * @param <E> the enum type
	 * @param enumClass the enum class
	 * @return the enum data type with a NOT NULL constraint
	 */
	public static <E extends Enum<E>> String javaToSqlEnum(Class<E> enumClass) {
		StringBuilder builder = new StringBuilder("ENUM (");
		E[] elements = enumClass.getEnumConstants();
		for (int n = 0; n < elements.length; n++) {
			if (n != 0) {
				builder.append(", ");
			}
			String name = elements[n].name();
			builder.append('\'').append(name).append('\'');
		}
		return builder.append(") NOT NULL").toString();
	}
	
	/**
	 * Convenience method to return the current unix time in seconds.
	 * Uses <code>System.currentTimeMillis()</code>
	 * 
	 * @return the current unix timestamp
	 */
	public static long currentTime() {
		return System.currentTimeMillis() / 1_000L;
	}
	
	/**
	 * Validates a Punishment is an own implementation
	 * 
	 * @param punishment
	 */
	static void validate(Punishment punishment) {
		if (!(punishment instanceof SecurePunishment)) {
			throw new IllegalArgumentException("Foreign implementation " + punishment.getClass());
		}
	}
	
	/**
	 * Validates that start and ends values are within range of SQL's INT UNSIGNED data type.
	 * 
	 * @param draftPunishment the draft punishment
	 * @throws IllegalArgumentException if either the start or end value would not fit into an INT UNSIGNED
	 */
	static void validate(DraftPunishment draftPunishment) {
		if (draftPunishment.getStart() > INT_UNSIGNED_MAX_VALUE) {
			throw new IllegalArgumentException("DraftPunishment starts after 2106! start=" + draftPunishment.getStart());
		}
		if (draftPunishment.getEnd() > INT_UNSIGNED_MAX_VALUE) {
			throw new IllegalArgumentException("DraftPunishment ends after 2106! end=" + draftPunishment.getEnd());
		}
	}
	
	/**
	 * Gets the enactment procedure for a punishment type, one of the following: <br>
	 * banhammer, mutehammer, warntallier, kicktallier
	 * 
	 * @param type the punishment type
	 * @return the enactment procedure name
	 */
	public static String getEnactmentProcedure(PunishmentType type) {
		return type.getLowercaseName() + ((type.isSingular()) ? "hammer" : "tallier");
	}
	
}
