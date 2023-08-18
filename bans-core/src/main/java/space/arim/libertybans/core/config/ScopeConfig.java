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

package space.arim.libertybans.core.config;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.scope.ConfiguredScope;

import java.util.List;

@ConfHeader({
		"This file is for proxies like BungeeCord and Velocity. It is irrelevant for single servers.",
		"It controls scope-related settings for this particular server on the network.",
		"",
		"Unlike other configuration files, one should NOT copy the scope.yml across multiple server instances"
})
public interface ScopeConfig {

	@ConfKey("default-punishing-scope")
	@ConfComments({
			"The default scope used to punish players",
			"",
			"GLOBAL - uses the global scope",
			"THIS_SERVER - applies to this server only via the server name",
			"PRIMARY_CATEGORY - uses the first category listed in 'categories-applicable-to-this-server'.",
			"",
			"If you use PRIMARY_CATEGORY but no categories are configured, a warning is printed and THIS_SERVER is used."
	})
	@ConfDefault.DefaultString("GLOBAL")
	DefaultPunishingScope defaultPunishingScope();

	enum DefaultPunishingScope {
		GLOBAL,
		THIS_SERVER,
		PRIMARY_CATEGORY
	}

	@ConfKey("categories-applicable-to-this-server")
	@ConfComments({
			"The scope categories applicable to this server",
			"",
			"For example, multiple servers might fall under the 'kitpvp' category,",
			"then staff members may use '-category=kitpvp' to create punishment applying to these servers"
	})
	@ConfDefault.DefaultStrings({})
	List<String> categoriesApplicableToThisServer();

	@ConfKey("server-name")
	@SubSection
	ServerName serverName();

	@ConfHeader({
			"Controls how the name of this server is detected for use with the server scope.",
			"",
			"The server name should correspond to the name of the backend server as configured on the proxy.",
			"The name of the proxy itself is 'proxy' by default, unless overridden."
	})
	interface ServerName {

		@ConfKey("auto-detect")
		@ConfComments({
				"By default, we try to detect the name of this backend server using plugin messaging.",
				"Make sure 'use-plugin-messaging' is enabled in the config.yml for this detection to succeed.",
				"",
				"If running a proxy, the detected name becomes 'proxy'.",
				"",
				"Plugin messaging requires at least one player to have logged into the backend server.",
				"Auto detection may fail, for example, if you ban someone through the console but no one has joined yet.",
				"",
				"To disable auto detection, set this to false then configure 'override-value' to the server name you wish to use.",
				"Re-enabling this option may require a restart (/libertybans restart)"
		})
		@ConfDefault.DefaultBoolean(true)
		boolean autoDetect();

		@ConfKey("override-value")
		@ConfComments({
				"If auto detection is disabled, this option should be set to the name of the server.",
				"",
				"Server names should be unique, but this is not a strict requirement.",
				"If you want a scope applying to multiple servers, you should use categories instead."
		})
		@ConfDefault.DefaultString("myserver")
		String overrideValue();

		@ConfKey("fallback-if-auto-detect-fails")
		@ConfComments({
				"Auto detection requires a player to have logged in. But you might punish players, e.g. via console, before that.",
				"By default, if auto detection has not yet occurred, the global scope will be used as a fallback.",
				"The fallback scope may be configured here to something else."
		})
		@ConfDefault.DefaultString("*")
		ConfiguredScope fallbackIfAutoDetectFails();

	}

	@ConfKey("require-permissions")
	@ConfComments({
			"Whether to require permissions for using scopes:",
			"- libertybans.scope.global, libertybans.scope.server.<server>, and libertybans.scope.category.<category>",
			"  become requirements to use the relevant scopes explicitly.",
			"- libertybans.scope.default must be granted to use commands without a scope argument"
	})
	@ConfDefault.DefaultBoolean(false)
	boolean requirePermissions();

}
