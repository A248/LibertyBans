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

package space.arim.libertybans.core.commands.extra;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.config.ParsedDuration;

import java.util.Set;

@ConfHeader({
		"Other options relating to tab completion",
		"These settings require a restart (/libertybans restart) to take effect"
})
public interface TabCompletionConfig {

	@ConfKey("offline-player-names")
	@SubSection
	OfflinePlayerNames offlinePlayerNames();

	@ConfHeader({
			"Regards tab completing the names of players who have formerly joined",
			"This can be a bit heavy on memory for large servers, so it's disabled by default.",
			"To tune how long player names are retained for, see the retention-minutes option"})
	interface OfflinePlayerNames {

		@ConfComments("Whether to enable this feature")
		@ConfDefault.DefaultBoolean(false)
		boolean enable();

		@ConfKey("retention-minutes")
		@ConfComments({
				"What is the period in which recently joining players' names should be completed",
				"If a player has joined in the last specified amount of minutes, his or her name is tab-completed"})
		@ConfDefault.DefaultLong(60 * 72)
		long retentionMinutes();

		@ConfKey("cache-refresh-seconds")
		@ConfComments({
				"This feature is implemented using a cache. How often should the cache be refreshed?",
				"Shorter times mean more accurate tab completion but use slightly more performance"})
		@ConfDefault.DefaultLong(120)
		long cacheRefreshSeconds();

	}

	@ConfKey("use-only-players-on-same-server")
	@ConfComments({
			"This option is only relevant when LibertyBans is on a proxy",
			"If enabled, tab completion of online player names will exclude the names of players",
			"on different backend servers."})
	@ConfDefault.DefaultBoolean(true)
	boolean useOnlyPlayersOnSameServer();

	@ConfKey("punishment-durations")
	@SubSection
	PunishmentDurations punishmentDurations();

	@ConfHeader({
			"Enables tab-completing punishment durations. For example, '30d' in '/ban A248 30d'",
			""})
	interface PunishmentDurations {

		@ConfComments("Whether to enable this feature")
		@ConfDefault.DefaultBoolean(false)
		boolean enable();

		@ConfKey("durations-to-supply")
		@ConfComments({
				"Which duration permissions should be tab-completed?",
				"Specify all the durations which you want to tab complete.",
				"",
				"If duration permissions are enabled, only players who have permission to use a certain duration",
				"will have that duration tab-completed."})
		@ConfDefault.DefaultStrings({"perm", "30d", "10d", "4h"})
		Set<ParsedDuration> durationsToSupply();

	}

}
