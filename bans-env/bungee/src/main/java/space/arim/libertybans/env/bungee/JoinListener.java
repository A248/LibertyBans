/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;

@Singleton
public class JoinListener implements PlatformListener, Listener {

    private final Plugin plugin;
    private final Guardian guardian;
    private final BungeeEnforcer bungeeEnforcer;

    @Inject
    public JoinListener(Plugin plugin, Guardian guardian, BungeeEnforcer bungeeEnforcer) {
        this.plugin = plugin;
        this.guardian = guardian;
        this.bungeeEnforcer = bungeeEnforcer;
    }

    @Override
    public void register() {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void unregister() {
        plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PostLoginEvent event) {
        guardian.onJoin(event.getPlayer(), bungeeEnforcer);
    }

}
