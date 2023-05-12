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

package space.arim.libertybans.core.event;

import java.util.Objects;

import space.arim.libertybans.api.event.PunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.core.env.CmdSender;

public class PunishEventImpl extends AbstractCancellable implements PunishEvent {

	private DraftPunishment draftPunishment;
	private final CmdSender sender;

	public PunishEventImpl(DraftPunishment draftPunishment, CmdSender sender) {
		this.draftPunishment = Objects.requireNonNull(draftPunishment);
		this.sender = sender;
	}

	@Override
	public DraftPunishment getDraftPunishment() {
		return draftPunishment;
	}

	@Override
	public void setDraftPunishment(DraftPunishment draftPunishment) {
		this.draftPunishment = draftPunishment;
	}

	// For internal use by LibertyBans addons
	public CmdSender getSender() {
		return sender;
	}

}
