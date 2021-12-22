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
 * The victim of a punishment. More information on the specific details of a victim
 * is available through subclasses
 * 
 * @author A248
 *
 */
public abstract class Victim {
	
	Victim() {}
	
	/**
	 * Gets the type of the victim
	 * 
	 * @return the victim type
	 */
	public abstract VictimType getType();

	/**
	 * A victim type. Corresponds to the subclasses of {@code Victim}
	 * 
	 * @author A248
	 *
	 */
	public enum VictimType {
		
		/**
		 * A player, identified by UUID
		 * 
		 */
		PLAYER,
		/**
		 * An IP address
		 * 
		 */
		ADDRESS,
		/**
		 * A combination of a UUID and an IP address
		 *
		 */
		COMPOSITE;
		
	}
	
	@Override
	public abstract int hashCode();
	
	/**
	 * Evaluates whether this victim is the same as another
	 * 
	 */
	@Override
	public abstract boolean equals(Object object);
	
	@Override
	public abstract String toString();
	
}
