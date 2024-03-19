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

package space.arim.libertybans.env.standalone;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

final class StandaloneEnforcer implements EnvEnforcer<Void> {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<CommandDispatch> commandHandler;

	@Inject
	StandaloneEnforcer(FactoryOfTheFuture futuresFactory, Provider<CommandDispatch> commandHandler) {
		this.futuresFactory = futuresFactory;
		this.commandHandler = commandHandler;
	}

	@Override
	public CentralisedFuture<Void> sendToThoseWithPermission(String permission, ComponentLike message) {
		return completedVoid();
	}

	@Override
	public CentralisedFuture<Void> sendToThoseWithPermissionNoPrefix(String permission, ComponentLike message) {
		return completedVoid();
	}

	@Override
	public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<Void> callback) {
		return completedVoid();
	}

	@Override
	public CentralisedFuture<Void> doForAllPlayers(Consumer<Collection<? extends Void>> action) {
		return completedVoid();
	}

	@Override
	public void kickPlayer(Void player, Component message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <D> boolean sendPluginMessageIfListening(Void player, PluginMessage<D, ?> pluginMessage, D data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendMessageNoPrefix(Void player, ComponentLike message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CentralisedFuture<Void> enforceMatcher(TargetMatcher<Void> matcher) {
		return completedVoid();
	}

	@Override
	public UUID getUniqueIdFor(Void player) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetAddress getAddressFor(Void player) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNameFor(Void player) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasPermission(Void player, String permission) {
		return false;
	}

	@Override
	public CentralisedFuture<Void> executeConsoleCommand(String command) {
		commandHandler.get().accept(command);
		return completedVoid();
	}

	private CentralisedFuture<Void> completedVoid() {
		return futuresFactory.completedFuture(null);
	}

}
