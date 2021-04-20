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

	BAN,
	MUTE,
	WARN,
	KICK;
	
	/**
	 * If this punishment type is singular, i.e., whether there can only be 1 kind of it
	 * applying to a specific {@link Victim}. This is the case for bans and mutes. It is
	 * not the case for warns and kicks.
	 * 
	 * @return true if the type is singular, false otherwise
	 */
	public boolean isSingular() {
		return this == BAN || this == MUTE;
	}

	/**
	 * Gets the type as a string. Same as {@code name().toLowerCase(Locale.ROOT)}
	 *
	 * @return the name lowercased using the root locale
	 */
	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
	
}
