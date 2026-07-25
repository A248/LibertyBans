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
import jakarta.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.scope.ServerNameListener;
import space.arim.libertybans.core.selector.Guardian;

@Singleton
public final class JoinListener implements PlatformListener, Listener {

	private final Plugin plugin;
	private final Guardian guardian;
	private final ServerNameListener<Player, ?> serverNameListener;
	private final SpigotEnforcer spigotEnforcer;

	@Inject
	public JoinListener(Plugin plugin, Guardian guardian,
						ServerNameListener<Player, ?> serverNameListener, SpigotEnforcer spigotEnforcer) {
		this.plugin = plugin;
        this.guardian = guardian;
        this.serverNameListener = serverNameListener;
        this.spigotEnforcer = spigotEnforcer;
    }

	@Override
	public void register() {
		serverNameListener.register();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
		serverNameListener.unregister();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		guardian.onJoin(player, spigotEnforcer);
		serverNameListener.onJoin(player, spigotEnforcer);
	}

}
