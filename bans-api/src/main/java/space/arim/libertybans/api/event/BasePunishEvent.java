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

import space.arim.libertybans.api.punish.DraftSanction;
import space.arim.omnibus.events.AsyncEvent;
import space.arim.omnibus.events.Cancellable;

/**
 * Parent interface for when a staff member is enacting a punishment. See {@link PunishEvent} and
 * {@link CalculatedPunishEvent}
 *
 * @param <S> the draft sanction type
 */
public interface BasePunishEvent<S extends DraftSanction> extends AsyncEvent, Cancellable {

	/**
	 * Gets the draft sanction which will be put into place
	 *
	 * @return the draft sanction
	 */
	S getDraftSanction();

	/**
	 * Sets the draft sanction which will be put into place
	 *
	 * @param draftSanction the new draft sanction
	 */
	void setDraftSanction(S draftSanction);

}
