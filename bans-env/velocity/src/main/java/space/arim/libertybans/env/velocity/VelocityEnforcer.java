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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Singleton
public class VelocityEnforcer extends AbstractEnvEnforcer<Player> {

	private final ProxyServer server;
	private final VelocityMessageChannel messageChannel;

	@Inject
	public VelocityEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter,
							Interlocutor interlocutor, ProxyServer server, VelocityMessageChannel messageChannel) {
		super(futuresFactory, formatter, interlocutor, AudienceRepresenter.identity());
		this.server = server;
		this.messageChannel = messageChannel;
	}

	@Override
	public CentralisedFuture<Void> doForAllPlayers(Consumer<Collection<? extends Player>> callback) {
		callback.accept(server.getAllPlayers());
		return completedVoid();
	}

	@Override
	public void kickPlayer(Player player, Component message) {
		player.disconnect(message);
	}

	@Override
	public <D> boolean sendPluginMessageIfListening(Player player, PluginMessage<D, ?> pluginMessage, D data) {
		messageChannel.sendPluginMessage(player, pluginMessage, data);
		return true;
	}

	@Override
	public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<Player> callback) {
		server.getPlayer(uuid).ifPresent(callback);
		return completedVoid();
	}

	@Override
	public UUID getUniqueIdFor(Player player) {
		return player.getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(Player player) {
		return player.getRemoteAddress().getAddress();
	}

	@Override
	public String getNameFor(Player player) {
		return player.getUsername();
	}

	@Override
	public boolean hasPermission(Player player, String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public CompletableFuture<Void> executeConsoleCommand(String command) {
		return server.getCommandManager()
				.executeAsync(server.getConsoleCommandSource(), command)
				.handle((ignore, ex) -> {
					if (ex != null) {
						Logger logger = LoggerFactory.getLogger(getClass());
						logger.warn("Exception occurred while executing console command {}", command, ex);
					}
					return null;
				});
	}

}
