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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;

@Singleton
public class JoinListener implements PlatformListener {

    private final PluginContainer plugin;
    private final ProxyServer server;
    private final Guardian guardian;
    private final VelocityEnforcer velocityEnforcer;

    @Inject
    public JoinListener(PluginContainer plugin, ProxyServer server, Guardian guardian, VelocityEnforcer velocityEnforcer) {
        this.plugin = plugin;
        this.server = server;
        this.guardian = guardian;
        this.velocityEnforcer = velocityEnforcer;
    }

    @Override
    public void register() {
        server.getEventManager().register(plugin, this);
    }

    @Override
    public void unregister() {
        server.getEventManager().unregisterListener(plugin, this);
    }

    @Subscribe(order = PostOrder.EARLY)
    public EventTask onJoin(PlayerChatEvent event) {
        guardian.onJoin(event.getPlayer(), velocityEnforcer);
        return null;
    }

}
