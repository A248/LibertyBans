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
package space.arim.libertybans.api.event;

import space.arim.omnibus.events.AsyncEvent;
import space.arim.omnibus.events.Cancellable;

import space.arim.libertybans.api.punish.DraftPunishment;

/**
 * Called when a staff member is enacting a punishment (/ban, /mute, etc.)
 * 
 * @author A248
 *
 */
public interface PunishEvent extends Cancellable, AsyncEvent {

	/**
	 * Gets the draft punishment which will be put into place. <br>
	 * <br>
	 * The draft punishment includes the operator who is enacting this punishment,
	 * the victim who is being punished, and several other details.
	 * 
	 * @return the draft punishment 
	 */
	DraftPunishment getDraftPunishment();

}
