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
import net.kyori.adventure.text.Component;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.env.sponge.listener.RegisterListeners;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

@Singleton
public final class ChatListener implements PlatformListener {

	private final RegisterListeners registerListeners;
	private final Guardian guardian;

	@Inject
	public ChatListener(RegisterListeners registerListeners, Guardian guardian) {
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

	@Listener(order = Order.LATE)
	public void onChat(PlayerChatEvent event) {
		combinedChatEvent(event, null);
	}

	@Listener(order = Order.LATE)
	public void onCommand(ExecuteCommandEvent.Pre event) {
		combinedChatEvent(event, event.command());
	}

	private <E extends Cancellable & Event> void combinedChatEvent(E event, String command) {
		if (!(event.cause().root() instanceof ServerPlayer player)) {
			return;
		}
		if (event.isCancelled()) {
			return;
		}
		CentralisedFuture<Component> futureMessage = guardian.checkChat(
				player.uniqueId(), player.connection().address().getAddress(), command
		);
		Component message;
		if (futureMessage.isDone()) {
			message = futureMessage.join();
		} else {
			throw new IllegalStateException("Cached mute unavailable for synchronous chat/command event");
		}
		if (message == null) {
			return;
		}
		event.setCancelled(true);
		player.sendMessage(message);
	}
}
