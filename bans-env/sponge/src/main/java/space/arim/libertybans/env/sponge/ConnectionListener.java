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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import space.arim.libertybans.core.env.ParallelisedListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.env.sponge.listener.RegisterListeners;

import java.net.InetAddress;
import java.util.UUID;

@Singleton
public final class ConnectionListener extends ParallelisedListener<ServerSideConnectionEvent.Handshake, Component> {

	private final RegisterListeners registerListeners;
	private final Guardian guardian;

	@Inject
	public ConnectionListener(RegisterListeners registerListeners, Guardian guardian) {
		this.registerListeners = registerListeners;
		this.guardian = guardian;
	}

	@Override
	public void register() {
		registerListeners.register(this);
	}

	@Override
	public void unregister() {
		registerListeners.unregister(this);
	}

	@Listener(order = Order.EARLY)
	public void onConnectEarly(ServerSideConnectionEvent.Handshake event) {
		UUID uuid = event.profile().uniqueId();
		String name = event.profile().name().orElseThrow(() -> new IllegalStateException("No name found"));
		InetAddress address = event.connection().address().getAddress();
		begin(event, guardian.executeAndCheckConnection(uuid, name, address));
	}

	@Override
	protected boolean isAllowed(ServerSideConnectionEvent.Handshake event) {
		// No way to check if the connection has been closed by someone else
		return true;
	}

	@Listener(order = Order.LATE)
	public void onConnectLate(ServerSideConnectionEvent.Handshake event) {
		Component message = withdraw(event);
		if (message == null) {
			debugResultPermitted(event);
			return;
		}
		event.connection().close(message);
	}

}
