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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.slf4j.LoggerFactory;
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
public final class ChatListener {

	private final RegisterListeners registerListeners;
	private final Guardian guardian;

	@Inject
	public ChatListener(RegisterListeners registerListeners, Guardian guardian) {
		this.registerListeners = registerListeners;
		this.guardian = guardian;
	}

	/*
	API differences require us to support everyone

	In Sponge API 8 and 9, PlayerChatEvent is Cancellable and handled by us
	In Sponge API 13, PlayerChatEvent.Submit becomes the event we need to listen to

	So we split the implementation based on runtime API detection
	 */

	PlatformListener provide() {
		try {
			Class.forName("org.spongepowered.api.event.message.PlayerChatEvent.Submit");
			return new SpongeApiThirteen();
		} catch (ClassNotFoundException lowerVersion) {
			LoggerFactory.getLogger(getClass()).warn(
					"You're running an older Sponge API version. We recommend updating if possible, since it is hard" +
							"to us to keep up with Sponge's frequent API changes and still support multiple versions."
			);
			return new SpongeApiEightOrNine();
		}
	}

	abstract class Common implements PlatformListener {

		@Override
		public void register() {
			registerListeners.register(this);
		}

		@Override
		public void unregister() {
			registerListeners.unregister(this);
		}

		<E extends Cancellable & Event> void combinedChatEvent(E event, String command) {
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

	public final class SpongeApiEightOrNine extends Common {

		@Listener(order = Order.LATE)
		public void onChat(PlayerChatEvent event) {
            combinedChatEvent((Cancellable & Event) event, null);
		}

		@Listener(order = Order.LATE)
		public void onCommand(ExecuteCommandEvent.Pre event) {
			combinedChatEvent(event, event.command());
		}
	}

	public final class SpongeApiThirteen extends Common {

		@Listener(order = Order.LATE)
		public void onChat(PlayerChatEvent.Submit event) {
			combinedChatEvent(event, null);
		}

		@Listener(order = Order.LATE)
		public void onCommand(ExecuteCommandEvent.Pre event) {
			combinedChatEvent(event, event.command());
		}
	}
}
