/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.api.event;

import space.arim.omnibus.events.AsyncEvent;
import space.arim.omnibus.events.Cancellable;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

/**
 * Called when a staff member is revoking a punishment (/unban, /unmute, etc.)
 * 
 * @author A248
 *
 */
public interface PardonEvent extends Cancellable, AsyncEvent {

	/**
	 * Gets the staff member responsible
	 * 
	 * @return the operator revoking the punishment
	 */
	Operator getOperator();

	/**
	 * Gets the victim who would be pardoned
	 * 
	 * @return the victim
	 */
	Victim getPardonedVictim();

	/**
	 * Gets the type of the punishment the victim is being pardoned for
	 * 
	 * @return the punishment type
	 */
	PunishmentType getPunishmentType();

}
