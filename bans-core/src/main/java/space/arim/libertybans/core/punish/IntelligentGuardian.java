/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public final class IntelligentGuardian implements Guardian {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final InternalSelector selector;
	private final UUIDManager uuidManager;
	private final MuteCache muteCache;

	@Inject
	public IntelligentGuardian(Configs configs, FactoryOfTheFuture futuresFactory,
							   InternalSelector selector, UUIDManager uuidManager, MuteCache muteCache) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.selector = selector;
		this.uuidManager = uuidManager;
		this.muteCache = muteCache;
	}

	@Override
	public CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address) {
		uuidManager.addCache(uuid, name);
		return selector.executeAndCheckConnection(uuid, name, address)
				.thenCompose((component) -> {
					// Contact the mute cache, but only if needed
					if (component != null) {
						return futuresFactory.completedFuture(component);
					}
					return muteCache.cacheOnLogin(uuid, address).thenApply((ignore) -> null);
				})
				.orTimeout(12, TimeUnit.SECONDS);
	}

	@Override
	public CentralisedFuture<Component> checkChat(UUID uuid, NetworkAddress address, String command) {
		if (command != null && !blockForMuted(command)) {
			return futuresFactory.completedFuture(null);
		}
		return muteCache.getCachedMuteMessage(uuid, address).thenApply((opt) -> opt.orElse(null));
	}

	private boolean blockForMuted(String command) {
		String[] words = command.split(" ");
		// Handle commands with colons
		if (words[0].indexOf(':') != -1) {
			words[0] = words[0].split(":", 2)[1];
		}
		for (String muteCommand : configs.getMainConfig().enforcement().muteCommands()) {
			if (muteCommandMatches(words, muteCommand)) {
				return true;
			}
		}
		return false;
	}

	private static boolean muteCommandMatches(String[] commandWords, String muteCommand) {
		// Basic equality check
		if (commandWords[0].equalsIgnoreCase(muteCommand)) {
			return true;
		}
		// Advanced equality check
		// Essentially a case-insensitive "startsWith" for arrays
		if (muteCommand.indexOf(' ') != -1) {
			String[] muteCommandWords = muteCommand.split(" ");
			if (muteCommandWords.length > commandWords.length) {
				return false;
			}
			for (int n = 0; n < muteCommandWords.length; n++) {
				if (!muteCommandWords[n].equalsIgnoreCase(commandWords[n])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
