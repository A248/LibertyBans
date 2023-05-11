/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Victim;

/**
 * Base interface for all sanctions. A sanction, as opposed to a full punishment, need not have
 * specific details such as duration, type, reason, or scope specified.
 *
 */
public interface SanctionBase {

	/**
	 * Gets the victim of the sanction or punishment
	 *
	 * @return the victim
	 */
	Victim getVictim();

	/**
	 * Gets the operator of the sanction or punishment
	 *
	 * @return the operator
	 */
	Operator getOperator();

}
