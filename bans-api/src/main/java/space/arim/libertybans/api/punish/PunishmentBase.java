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
package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;

/**
 * Base interface for {@link DraftPunishment} and {@link Punishment}
 * 
 * @author A248
 *
 */
public interface PunishmentBase {

	/**
	 * Gets the type of the punishment
	 * 
	 * @return the type
	 */
	PunishmentType getType();
	
	/**
	 * Gets the victim of the punishment
	 * 
	 * @return the victim
	 */
	Victim getVictim();
	
	/**
	 * Gets the operator of the punishment
	 * 
	 * @return the operator
	 */
	Operator getOperator();
	
	/**
	 * Gets the reason of the punishment
	 * 
	 * @return the reason
	 */
	String getReason();
	
	/**
	 * Gets the scope of the punishment
	 * 
	 * @return the scope
	 */
	ServerScope getScope();
	
}
