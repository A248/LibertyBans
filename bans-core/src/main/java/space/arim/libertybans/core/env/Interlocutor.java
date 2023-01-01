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

package space.arim.libertybans.core.env;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;

import java.util.regex.Pattern;

/**
 * Implements IP address censorship
 */
public final class Interlocutor {

	private final Configs configs;

	static final String PERMISSION_TO_VIEW_IPS = "libertybans.admin.viewips";
	static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
			"""
			((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])|(((([0-9a-fA-F]){1,4})\\:){7}([0-9a-fA-F]){1,4})\
			""",
			Pattern.CASE_INSENSITIVE
	);

	@Inject
	public Interlocutor(Configs configs) {
		this.configs = configs;
	}

	private MessagesConfig.Formatting.VictimDisplay conf() {
		return configs.getMessagesConfig().formatting().victimDisplay();
	}

	/**
	 * Filters IP addresses if the sender does not have permission
	 *
	 * @param sender the command sender
	 * @param message the message to filter
	 * @return the new message if necessary
	 */
	public ComponentLike filterIpAddresses(CmdSender sender, ComponentLike message) {
		if (!sender.hasPermission(PERMISSION_TO_VIEW_IPS) && shouldFilterIpAddresses()) {
			return stripIpAddresses(message);
		}
		return message;
	}

	/**
	 * Removes IP addresses from the given message
	 *
	 * @param message the message
	 * @return the new message
	 */
	public Component stripIpAddresses(ComponentLike message) {
		return message.asComponent().replaceText((config) -> {
			config.match(IP_ADDRESS_PATTERN).replacement(conf().censoredIpAddress());
		});
	}

	/**
	 * Whether IP address censorship is enabled
	 *
	 * @return true if censorship is enabled
	 */
	public boolean shouldFilterIpAddresses() {
		return conf().censorIpAddresses();
	}

}
