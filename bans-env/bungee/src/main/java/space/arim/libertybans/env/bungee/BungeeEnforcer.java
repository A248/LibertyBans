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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
import java.util.function.Consumer;

@Singleton
public class BungeeEnforcer extends AbstractEnvEnforcer<ProxiedPlayer> {

	private final ProxyServer server;
	private final AddressReporter addressReporter;
	private final BungeeMessageChannel messageChannel;

	@Inject
	public BungeeEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter,
						  Interlocutor interlocutor, AudienceRepresenter<CommandSender> audienceRepresenter,
						  ProxyServer server, AddressReporter addressReporter, BungeeMessageChannel messageChannel) {
		super(futuresFactory, formatter, interlocutor, audienceRepresenter);
		this.server = server;
		this.addressReporter = addressReporter;
		this.messageChannel = messageChannel;
	}

	@Override
	public CentralisedFuture<Void> doForAllPlayers(Consumer<Collection<? extends ProxiedPlayer>> callback) {
		callback.accept(server.getPlayers());
		return completedVoid();
	}

	@Override
	public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<ProxiedPlayer> callback) {
		ProxiedPlayer player = server.getPlayer(uuid);
		if (player != null) {
			callback.accept(player);
		}
		return completedVoid();
	}

	@Override
	public void kickPlayer(ProxiedPlayer player, Component message) {
		player.disconnect(TextComponent.fromLegacyText(
				LegacyComponentSerializer.legacySection().serialize(message)));
	}

	@Override
	public <D> boolean sendPluginMessageIfListening(ProxiedPlayer player, PluginMessage<D, ?> pluginMessage, D data) {
		messageChannel.sendPluginMessage(player, pluginMessage, data);
		return true;
	}

	@Override
	public UUID getUniqueIdFor(ProxiedPlayer player) {
		return player.getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(ProxiedPlayer player) {
		return addressReporter.getAddress(player);
	}

	@Override
	public String getNameFor(ProxiedPlayer player) {
		return player.getName();
	}

	@Override
	public boolean hasPermission(ProxiedPlayer player, String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public CentralisedFuture<Void> executeConsoleCommand(String command) {
		server.getPluginManager().dispatchCommand(server.getConsole(), command);
		return completedVoid();
	}

}
