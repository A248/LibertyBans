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
package space.arim.libertybans.it.env.platform;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.jsonchat.adventure.implementor.MessageOnlyAudience;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class QuackPlayer implements MessageOnlyAudience {

	private final QuackPlayerStore playerStore;
	private final UUID uuid;
	private final String name;
	private final InetAddress address;
	private @Nullable String playableServerName;
	
	private final Set<String> permissions;
	private final Set<ReceivedPluginMessage<?>> receivedPluginMessages = new HashSet<>();
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	QuackPlayer(QuackPlayerStore playerStore, UUID uuid, String name, InetAddress address,
				Set<String> permissions) {
		this.playerStore = playerStore;
		this.uuid = uuid;
		this.name = name;
		this.address = address;
		this.permissions = permissions;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public InetAddress getAddress() {
		return address;
	}

    public NetworkAddress getNetworkAddress() {
        return NetworkAddress.of(address);
    }

	public boolean isOnline() {
		return playerStore.getPlayer(uuid).isPresent();
	}
	
	public boolean hasPermission(String permission) {
		return permissions.contains(permission);
	}
	
	public void kickPlayer(Component message) {
		playerStore.remove(this);
		logger.info("{} was kicked for '{}'", getName(), QuackPlatform.toDisplay(message));
	}

	public Set<ReceivedPluginMessage<?>> receivedPluginMessages() {
		return receivedPluginMessages;
	}

	public @Nullable String getPlayableServerName() {
		return playableServerName;
	}

	public void setPlayableServerName(@Nullable String playableServerName) {
		this.playableServerName = playableServerName;
	}

	@Override
	public void sendMessage(@NonNull Identity source, @NonNull Component message, @NonNull MessageType type) {
		String displayMessage = QuackPlatform.toDisplay(message);
		if (source.equals(Identity.nil())) {
			logger.info("{} received {} '{}'", name, type, displayMessage);
		} else {
			logger.info("{} received {} '{}' from {}", name, type, displayMessage, source);
		}
	}

	@Override
	public UnsupportedOperationException notSupportedException() {
		return new UnsupportedOperationException("Not supported");
	}
}
