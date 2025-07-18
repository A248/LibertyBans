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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.scope.ServerNameListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.env.sponge.listener.RegisterListeners;

@Singleton
public final class JoinListener implements PlatformListener {

	private final RegisterListeners registerListeners;
	private final Guardian guardian;
	private final ServerNameListener<ServerPlayer, ?> serverNameListener;
	private final SpongeEnforcer spongeEnforcer;

	@Inject
	public JoinListener(RegisterListeners registerListeners, Guardian guardian,
						ServerNameListener<ServerPlayer, ?> serverNameListener, SpongeEnforcer spongeEnforcer) {
		this.registerListeners = registerListeners;
        this.guardian = guardian;
        this.serverNameListener = serverNameListener;
        this.spongeEnforcer = spongeEnforcer;
    }

	@Override
	public void register() {
		serverNameListener.register();
		registerListeners.register(this);
	}

	@Override
	public void unregister() {
		registerListeners.unregister(this);
		serverNameListener.unregister();
	}

	@Listener(order = Order.EARLY)
	public void onJoin(ServerSideConnectionEvent.Join event) {
		ServerPlayer player = event.player();
		guardian.onJoin(player, spongeEnforcer);
		serverNameListener.onJoin(player, spongeEnforcer);
	}

}
