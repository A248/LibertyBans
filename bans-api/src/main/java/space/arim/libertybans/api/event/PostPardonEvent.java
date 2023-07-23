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

package space.arim.libertybans.api.event;

import space.arim.omnibus.events.AsyncEvent;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.punish.Punishment;

import java.util.Optional;

/**
 * Called once a punishment has been revoked
 * 
 * @author A248
 *
 */
public interface PostPardonEvent extends AsyncEvent {

	/**
	 * Gets the staff member who revoked the punishment
	 * 
	 * @return the operator responsible for the pardon
	 */
	Operator getOperator();

	/**
	 * Gets the punishment revoked
	 * 
	 * @return the punishment which was revoked
	 */
	Punishment getPunishment();

	/**
	 * If this event was the result of a command line action, the targeted user may be available in many circumstances
	 *
	 * @return the targeted user (victim name) on the command line, if available
	 */
	default Optional<String> getTarget() {
		return Optional.empty();
	}

}
