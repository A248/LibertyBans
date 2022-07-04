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
import space.arim.libertybans.core.env.TargetMatcher;

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class BungeeEnforcer extends AbstractEnvEnforcer<CommandSender, ProxiedPlayer> {

	private final ProxyServer server;
	private final AddressReporter addressReporter;

	@Inject
	public BungeeEnforcer(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
						  ProxyServer server, AddressReporter addressReporter) {
		super(formatter, audienceRepresenter);
		this.server = server;
		this.addressReporter = addressReporter;
	}

	@Override
	protected void sendToThoseWithPermissionNoPrefix(String permission, Component message) {
		for (ProxiedPlayer player : server.getPlayers()) {
			if (player.hasPermission(permission)) {
				audienceRepresenter().toAudience(player).sendMessage(message);
			}
		}
	}

	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<ProxiedPlayer> callback) {
		ProxiedPlayer player = server.getPlayer(uuid);
		if (player != null) {
			callback.accept(player);
		}
	}

	@Override
	public void kickPlayer(ProxiedPlayer player, Component message) {
		player.disconnect(TextComponent.fromLegacyText(
				LegacyComponentSerializer.legacySection().serialize(message)));
	}

	@Override
	public void enforceMatcher(TargetMatcher<ProxiedPlayer> matcher) {
		for (ProxiedPlayer player : server.getPlayers()) {
			if (matcher.matches(player.getUniqueId(), addressReporter.getAddress(player))) {
				matcher.callback().accept(player);
			}
		}
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
	public void executeConsoleCommand(String command) {
		server.getPluginManager().dispatchCommand(server.getConsole(), command);
	}

}
