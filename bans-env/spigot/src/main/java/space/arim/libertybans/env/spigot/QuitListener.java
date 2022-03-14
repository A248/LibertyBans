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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.cache.MuteCache;

@Singleton
public final class QuitListener implements PlatformListener, Listener {

	private final JavaPlugin plugin;
	private final MuteCache muteCache;

	@Inject
	public QuitListener(JavaPlugin plugin, MuteCache muteCache) {
		this.plugin = plugin;
		this.muteCache = muteCache;
	}

	@Override
	public void register() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		muteCache.uncacheOnQuit(player.getUniqueId(), NetworkAddress.of(player.getAddress().getAddress()));
	}
}
