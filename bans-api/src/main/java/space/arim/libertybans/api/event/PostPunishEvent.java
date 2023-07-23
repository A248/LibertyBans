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

import space.arim.libertybans.api.punish.Punishment;

import java.util.Optional;

/**
 * Called after a punishment has been enacted
 * 
 * @author A248
 *
 */
public interface PostPunishEvent extends AsyncEvent {

	/**
	 * Gets the punishment which was put into place. <br>
	 * <br>
	 * The punishment includes the operator who is enacting this punishment, the
	 * victim who is being punished, and several other details.
	 * 
	 * @return the punishment
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
