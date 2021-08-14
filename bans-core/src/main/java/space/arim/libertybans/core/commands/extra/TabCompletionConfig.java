/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.commands.extra;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;

@ConfHeader({
		"Other options relating to tab completion",
		"These settings require a restart (/libertybans restart) to take effect"
})
public interface TabCompletionConfig {

	@ConfKey("offline-player-names")
	@ConfComments({
			"Whether to tab complete the names of players who have formerly joined",
			"This can be a bit heavy on memory for large servers, so it's disabled by default.",
			"To tune how long player names are retained for, see offline-player-names-retention-hours"})
	@ConfDefault.DefaultBoolean(false)
	boolean offlinePlayerNames();

	@ConfKey("offline-player-names-retention-hours")
	@ConfComments({
			"If offline-player-names is enabled, what is the period in which recently joining players' names should be completed",
			"If a player has joined in the last specified amount of hours, his or her name is tab-completed"})
	@ConfDefault.DefaultInteger(72)
	int offlinePlayerNamesRetentionHours();

}
