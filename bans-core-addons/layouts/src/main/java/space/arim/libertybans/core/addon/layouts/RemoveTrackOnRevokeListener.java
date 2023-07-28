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

package space.arim.libertybans.core.addon.layouts;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.event.PostPardonEvent;
import space.arim.omnibus.events.EventFireController;
import space.arim.omnibus.events.ListenerPriorities;
import space.arim.omnibus.events.ListeningMethod;
import space.arim.omnibus.util.ThisClass;

@Singleton
public final class RemoveTrackOnRevokeListener {

	private final LayoutsAddon addon;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public RemoveTrackOnRevokeListener(LayoutsAddon addon) {
		this.addon = addon;
	}

	@ListeningMethod(priority = ListenerPriorities.HIGH)
	public void onRevoke(PostPardonEvent event, EventFireController controller) {
		if (!addon.config().clearTrackWhenPunishmentRevoked()) {
			controller.continueFire();
			return;
		}
		event.getPunishment().modifyPunishment((editor) -> {
			// Remove track
			editor.setEscalationTrack(null);
		}).whenComplete((ignore, ex) -> {
			if (ex != null) {
				logger.warn("Exception while modifying punishment", ex);
			}
			controller.continueFire();
		});
	}

}
