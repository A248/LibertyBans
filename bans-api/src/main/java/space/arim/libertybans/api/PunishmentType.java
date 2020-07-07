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
package space.arim.libertybans.api;

import java.util.Locale;

/**
 * The type of a punishment
 * 
 * @author A248
 *
 */
public enum PunishmentType {

	BAN(true),
	MUTE(true),
	WARN(false),
	KICK(false);
	
	private final boolean singular;
	
	private PunishmentType(boolean singular) {
		this.singular = singular;
	}
	
	/**
	 * If this punishment type is singular, i.e., whether there can only be 1 kind of it
	 * applying to a specific {@link Victim}. This is the case for bans and mutes. It is
	 * not the case for warns and kicks.
	 * 
	 * @return true if the type is singular, false otherwise
	 */
	public boolean isSingular() {
		return singular;
	}
	
	/**
	 * Shortcut for {@link #name()} lowercased using the english locale
	 * 
	 * @return the lowercased name
	 */
	public String getLowercaseName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
	
	/**
	 * Shortcut for <code>getLowercaseName() + 's'</code>
	 * 
	 * @return the lowercased name with 's' appended
	 */
	public String getLowercaseNamePlural() {
		return getLowercaseName() + 's';
	}
	
	/**
	 * Gets a PunishmentType from an ordinal, or {@code null} if no such
	 * ordinal exists in the enum
	 * 
	 * @param ordinal the ordinal, 0, 1, 2, 3
	 * @return the corresponding punishment type, or {@code null}
	 */
	public static PunishmentType fromOrdinal(int ordinal) {
		switch (ordinal) {
		case 0:
			return BAN;
		case 1:
			return MUTE;
		case 2:
			return WARN;
		case 3:
			return KICK;
		default:
			return null;
		}
	}
	
	@Override
	public String toString() {
		String name = name();
		return name.charAt(0) + name.substring(1).toLowerCase();
	}
	
}
