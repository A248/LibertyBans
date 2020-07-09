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

/**
 * A full punishment, identifiable by its ID
 * 
 * @author A248
 *
 */
public interface Punishment extends PunishmentBase {

	/**
	 * Gets the unique ID of this punishment
	 * 
	 * @return the ID of the punishment
	 */
	int getID();
	
	/**
	 * Whether this punishment is equal to another. Implementations
	 * need only check {@link #getID()} since IDs must always be unique.
	 * 
	 * @param object the other object
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	boolean equals(Object object);
	
}
