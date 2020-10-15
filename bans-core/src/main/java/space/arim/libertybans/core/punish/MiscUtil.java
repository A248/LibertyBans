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
package space.arim.libertybans.core.punish;

import java.util.List;
import java.util.Objects;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.core.selector.AddressStrictness;
import space.arim.libertybans.core.selector.SyncEnforcement;

public final class MiscUtil {
	
	/**
	 * The biggest value which can be stored in a 32bit unsigned integer
	 */
	private static final long INT_UNSIGNED_MAX_VALUE = ((long) Integer.MAX_VALUE) - ((long) Integer.MIN_VALUE);
	
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
	 * <b>DO NOT MUTATE the result</b>
	 * 
	 * @return the cached result of {@link PunishmentType#values()} excluding {@code KICK}
	 */
	public static PunishmentType[] punishmentTypesExcludingKick() {
		return PUNISHMENT_TYPES_EXCLUDING_KICK;
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
	 * Validates that start and ends values are within range of a 32bit unsigned integer.
	 * 
	 * @param start the start time
	 * @param end the end time
	 * @throws IllegalArgumentException if either the start or end value would not fit into an INT UNSIGNED
	 */
	static void checkRange(long start, long end) {
		if (start > INT_UNSIGNED_MAX_VALUE) {
			throw new IllegalArgumentException("Start time is after 2106. start=" + start);
		}
		if (end > INT_UNSIGNED_MAX_VALUE) {
			throw new IllegalArgumentException("End time is after 2106. end=" + end);
		}
	}
	
	/**
	 * Checks that a server scope is nonnull and of the right implementation class
	 * 
	 * @param scope the server scope
	 * @return the same scope, for convenience
	 * @throws NullPointerException if {@code scope} is null
	 * @throws IllegalArgumentException if {@code scope} is a foreign implementation
	 */
	static ServerScope checkScope(ServerScope scope) {
		Scoper.checkScope(scope);
		return scope;
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
	
	/**
	 * Checks whether the specified end time is expired based on the current time, i.e.
	 * {@code end != 0} and {@code end < currentTime}
	 * 
	 * @param currentTime the current time
	 * @param end the end time
	 * @return true if expired, false otherwise
	 */
	static boolean isExpired(long currentTime, long end) {
		return end != 0 && end < currentTime;
	}
	
	/**
	 * Checks that {@link PunishmentType#isSingular()} is true
	 * 
	 * @param type the punishment type
	 * @return the same punishment type, for convenience
	 * @throws NullPointerException if {@code type} is null
	 * @throws IllegalArgumentException if {@code type} is not singular
	 */
	static PunishmentType checkSingular(PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (!type.isSingular()) {
			throw new IllegalArgumentException("The punishment type " + type + " is not singular");
		}
		return type;
	}
	
	/*
	 * Create exceptions indicating an enum entry was not identified, typically in a switch statement
	 */

	public static RuntimeException unknownType(PunishmentType type) {
		return unknownEnumEntry(type);
	}

	public static RuntimeException unknownVictimType(Victim.VictimType victimType) {
		return unknownEnumEntry(victimType);
	}

	public static RuntimeException unknownOperatorType(Operator.OperatorType operatorType) {
		return unknownEnumEntry(operatorType);
	}
	
	public static RuntimeException unknownAddressStrictness(AddressStrictness strictness) {
		return unknownEnumEntry(strictness);
	}
	
	public static RuntimeException unknownSyncEnforcement(SyncEnforcement strategy) {
		return unknownEnumEntry(strategy);
	}
	
	public static RuntimeException unknownVendor(Vendor vendor) {
		return unknownEnumEntry(vendor);
	}
	
	static UnknownEnumEntryException unknownEnumEntry(Enum<?> value) {
		return new UnknownEnumEntryException(value);
	}
	
	private static class UnknownEnumEntryException extends RuntimeException {

		/**
		 * Serial version uid
		 */
		private static final long serialVersionUID = 5616757616849475684L;
		
		UnknownEnumEntryException(Enum<?> value) {
			super("Unknown enum entry " + value.name() + " in " + value.getDeclaringClass().getName());
		}
		
	}
	
}
