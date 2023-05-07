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

package space.arim.libertybans.core.addon.shortcutreasons;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.event.PunishEvent;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.core.event.PunishEventImpl;
import space.arim.omnibus.events.ListeningMethod;

@Singleton
public final class ShortcutReasonsListener {

	private final ShortcutReasonsAddon addon;

	@Inject
	public ShortcutReasonsListener(ShortcutReasonsAddon addon) {
		this.addon = addon;
	}

	@ListeningMethod
	public void onPunish(PunishEvent event) {
		ShortcutReasonsConfig config = addon.config();
		DraftPunishment originalPunishment = event.getDraftSanction();
		String originalReason = originalPunishment.getReason();
		String shortcutIdentifier = config.shortcutIdentifier();
		if (!originalReason.startsWith(shortcutIdentifier)) {
			return;
		}
		String newReason = config.shortcuts().get(originalReason.substring(shortcutIdentifier.length()));
		if (newReason == null) {
			((PunishEventImpl) event).getSender().sendMessage(
					config.doesNotExist().replaceText("%SHORTCUT_ARG%", originalReason)
			);
			event.cancel();
			return;
		}
		DraftPunishment newPunishment = originalPunishment.toBuilder().reason(newReason).build();
		event.setDraftSanction(newPunishment);
	}

}
