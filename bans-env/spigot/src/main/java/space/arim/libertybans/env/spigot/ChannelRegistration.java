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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import org.bukkit.plugin.Plugin;
import space.arim.libertybans.core.env.PlatformListener;

public final class ChannelRegistration implements PlatformListener {

	private final Plugin plugin;

	static final String BUNGEE_CHANNEL = "BungeeCord";

	@Inject
	public ChannelRegistration(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void register() {
		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
	}

	@Override
	public void unregister() {
		plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
	}

}
