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
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;

import java.util.Optional;

/**
 * Base interface for punishments
 *
 */
public interface PunishmentBase extends SanctionBase {

	/**
	 * Gets the type of the punishment
	 * 
	 * @return the type
	 */
	PunishmentType getType();

	/*
	Redeclare victim and operator methods from SanctionBase for 1.0 API compatibility
	 */

	@Override
	Victim getVictim();

	@Override
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

	/**
	 * Gets the escalation track of the punishment if there is one
	 *
	 * @return the escalation track if set
	 */
	Optional<EscalationTrack> getEscalationTrack();

}
