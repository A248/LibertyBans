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

import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftSanction;

/**
 * Called when a staff member is enacting a punishment with standard punishment commands (/ban, /mute, etc.)
 * For intercepting punishments created by layouts, see {@link CalculatedPunishEvent}
 *
 */
public interface PunishEvent extends BasePunishEvent<DraftPunishment> {

	/**
	 * Gets the draft punishment which will be put into place. <br>
	 * <br>
	 * The draft punishment includes the operator who is enacting this punishment,
	 * the victim who is being punished, and several other details.
	 * 
	 * @return the draft punishment
	 * @deprecated The equivalent getter {@link #getDraftSanction()} should be preferred
	 */
	@Deprecated
	default DraftPunishment getDraftPunishment() {
		return getDraftSanction();
	}

	/**
	 * Sets the draft punishment which will be put into place. <br>
	 *
	 * The draft punishment includes the operator who is enacting this punishment,
	 * the victim who is being punished, and several other details.
	 *
	 * @param draftPunishment the new draft punishment
	 * @deprecated The equivalent setter {@link #setDraftSanction(DraftSanction)} should be preferred
	 */
	@Deprecated
	default void setDraftPunishment(DraftPunishment draftPunishment) {
		setDraftSanction(draftPunishment);
	}

}
