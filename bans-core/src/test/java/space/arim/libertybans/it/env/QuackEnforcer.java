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

package space.arim.libertybans.it.env;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;
import space.arim.libertybans.it.env.platform.ReceivedPluginMessage;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class QuackEnforcer extends AbstractEnvEnforcer<QuackPlayer> {

	private final QuackPlatform platform;

	@Inject
	public QuackEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter,
						 Interlocutor interlocutor, QuackPlatform platform) {
		super(futuresFactory, formatter, interlocutor, AudienceRepresenter.identity());
		this.platform = platform;
	}

	@Override
	public CentralisedFuture<Void> doForAllPlayers(Consumer<Collection<? extends QuackPlayer>> callback) {
		callback.accept(platform.getAllPlayers());
		return completedVoid();
	}

	@Override
	public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<QuackPlayer> callback) {
		platform.getPlayer(uuid).ifPresent(callback);
		return completedVoid();
	}

	@Override
	public void kickPlayer(QuackPlayer player, Component message) {
		player.kickPlayer(message);
	}

	@Override
	public <D> boolean sendPluginMessageIfListening(QuackPlayer player, PluginMessage<D, ?> pluginMessage, D data) {
		player.receivedPluginMessages().add(new ReceivedPluginMessage<>(pluginMessage, data));
		return true;
	}

	@Override
	public UUID getUniqueIdFor(QuackPlayer player) {
		return player.getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(QuackPlayer player) {
		return player.getAddress();
	}

	@Override
	public String getNameFor(QuackPlayer player) {
		return player.getName();
	}

	@Override
	public boolean hasPermission(QuackPlayer player, String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public CentralisedFuture<Void> executeConsoleCommand(String command) {
		throw new UnsupportedOperationException();
	}

}
