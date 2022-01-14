/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.alts;

import net.kyori.adventure.text.Component;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.NumericRange;

@ConfHeader({
		"Limits players connecting from the same IP address.",
		"",
		"The limiter works by counting the amount of joins from a given IP address within a recent timespan.",
		"Thus, it is not an absolute limit, but a limit on the rate of joins."
})
public interface ConnectionLimitConfig {

	@ConfComments("Whether to enable this feature")
	@ConfDefault.DefaultBoolean(false)
	boolean enable();

	@ConfComments("The limit to apply")
	@ConfDefault.DefaultInteger(5)
	@NumericRange(min = 1)
	int limit();

	@ConfKey("duration-seconds")
	@ConfComments({"What is the duration within which recent joins are counted against the limit?",
			"The default is 30 minutes and the value is specified in seconds."})
	@ConfDefault.DefaultInteger(1800)
	@NumericRange(min = 1)
	long durationSeconds();

	@ConfKey("denial-message")
	@ConfComments("The message when a player is denied from joining due to the limit")
	@ConfDefault.DefaultString("There have been too many connections from your IP address recently")
	Component message();
}
