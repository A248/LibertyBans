/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
public interface PostPardonEvent extends AsyncEvent, PostOpNotificationEvent {

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
	@Override
	Punishment getPunishment();

	/**
	 * The command line target of the removal.
	 * <p>
	 * This is not necessarily the same as the punished victim ({@link Punishment#getVictim()}), but rather the string
	 * argument interpreted to mean it. In some contexts this argument will be unavailable; for example, removing a
	 * punishment by its ID alone has no target argument.
	 *
	 * @return the targeted user (victim name) on the command line, if available
	 */
	@Override
	default Optional<String> getTarget() {
		return Optional.empty();
	}

	/**
	 * If the punishment was removed silently.
	 *
	 * @return true if silent
	 */
	@Override
	default boolean isSilent() {
		return false;
	}
}
