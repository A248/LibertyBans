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

import space.arim.libertybans.api.event.PunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.core.env.CmdSender;

public final class PunishEventImpl extends BasePunishEventImpl<DraftPunishment> implements PunishEvent {

	private final CmdSender sender;

	public PunishEventImpl(DraftPunishment draftPunishment, CmdSender sender) {
		super(draftPunishment);
		this.sender = sender;
	}

	// For internal use by LibertyBans addons
	public CmdSender getSender() {
		return sender;
	}

	@Override
	public String toString() {
		return "PunishEventImpl{" +
				"sender=" + sender +
				", draftSanction=" + getDraftSanction() +
				", cancelled=" + isCancelled() +
				'}';
	}

}
