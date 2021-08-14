/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.uuid;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfSerialisers;

import java.util.regex.Pattern;

@ConfHeader({"Options relating to finding UUIDs from names, and vice-versa",
	"LibertyBans will first check its own caches before using these resources"})
@ConfSerialisers(RemoteApiBundle.SerialiserImpl.class)
public interface UUIDResolutionConfig {

	@ConfKey("server-type")
	@ConfComments({"",
		"What kind of UUIDs do your players have?",
		"Available options are ONLINE, OFFLINE, MIXED, and GEYSER",
		"",
		"For most servers this will be 'ONLINE'",
		"For offline servers where all players have offline UUIDs, use OFFLINE",
		"For offline servers where some players have online and some have offline UUIDs, use MIXED",
		"",
		"For Geyser/Floodgate users: Set this to GEYSER"
		})
	@DefaultString("ONLINE")
	ServerType serverType();
	
	@ConfKey("web-api-resolvers")
	@ConfComments({"",
		"As a last resort, when LibertyBans cannot find a UUID or name, it will use an external web API",
		"Available options are 'MOJANG', 'ASHCON', and 'MCHEADS'. They will be queried sequentially in the order specified.",
		"(If the server is not in ONLINE mode, this option is ignored)"})
	@DefaultStrings("MOJANG")
	RemoteApiBundle remoteApis();

	@ConfKey("geyser-name-prefix")
	@ConfComments({
			"If using Geyser, set this to the prefix in front of bedrock players' names.",
			"Geyser users should also set the server-type option to MIXED",
			"This setting requires a restart (/libertybans restart) to take effect"})
	@ConfDefault.DefaultString("")
	String geyserNamePrefix();

	default NameValidator nameValidator() {
		String geyserNamePrefix = geyserNamePrefix();
		String quotedPrefix;
		if (geyserNamePrefix.isEmpty()) {
			quotedPrefix = ""; // Avoid quoting empty strings (bad regex practice)
		} else {
			quotedPrefix = "(" + Pattern.quote(geyserNamePrefix) + ")?";
		}
		String validNameRegex = quotedPrefix + "[a-zA-Z0-9_]*+";
		return new StandardNameValidator(Pattern.compile(validNameRegex));
	}
	
}
