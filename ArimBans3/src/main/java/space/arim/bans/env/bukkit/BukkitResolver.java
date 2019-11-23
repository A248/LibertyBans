/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.bukkit;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import space.arim.bans.api.exception.FetcherException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.util.Fetcher;
import space.arim.bans.env.Resolver;

public class BukkitResolver implements Resolver {
	
	private final BukkitEnv environment;
	
	private boolean internalFetcher;
	private boolean ashconFetcher;
	private boolean mojangFetcher;
	
	public BukkitResolver(final BukkitEnv environment) {
		this.environment = environment;
		refreshConfig();
	}
	
	@Override
	public UUID uuidFromName(final String name) throws PlayerNotFoundException {
		Objects.requireNonNull(name, "name must not be null!");
		try {
			UUID uuid1 = environment.center().cache().getUUID(name);
			return uuid1;
		} catch (MissingCacheException ex) {}
		if (internalFetcher) {
			for (final OfflinePlayer player : environment.plugin().getServer().getOfflinePlayers()) {
				if (player.getName().equalsIgnoreCase(name)) {
					UUID uuid2 = player.getUniqueId();
					environment.center().cache().update(uuid2, name, null);
					return uuid2;
				}
			}
		}
		if (ashconFetcher) {
			try {
				UUID uuid3 = Fetcher.ashconApi(name);
				environment.center().cache().update(uuid3, name, null);
				return uuid3;
			} catch (FetcherException ex) {}
		}
		if (mojangFetcher) {
			try {
				UUID uuid4 = Fetcher.mojangApi(name);
				environment.center().cache().update(uuid4, name, null);
				return uuid4;
			} catch (FetcherException ex) {}
		}
		throw new PlayerNotFoundException(name);
	}
	
	@Override
	public String nameFromUUID(final UUID playeruuid) throws PlayerNotFoundException {
		Objects.requireNonNull(playeruuid, "uuid must not be null!");
		try {
			String name1 = environment.center().cache().getName(playeruuid);
			return name1;
		} catch (MissingCacheException ex) {}
		if (internalFetcher) {
			for (final OfflinePlayer player : environment.plugin().getServer().getOfflinePlayers()) {
				if (player.getUniqueId().equals(playeruuid)) {
					String name2 = player.getName();
					environment.center().cache().update(playeruuid, name2, null);
					return name2;
				}
			}
		}
		if (ashconFetcher) {
			try {
				String name3 = Fetcher.ashconApi(playeruuid);
				environment.center().cache().update(playeruuid, name3, null);
				return name3;
			} catch (FetcherException ex) {}
		}
		if (mojangFetcher) {
			try {
				String name4 = Fetcher.mojangApi(playeruuid);
				environment.center().cache().update(playeruuid, name4, null);
				return name4;
			} catch (FetcherException ex) {}
		}
		throw new PlayerNotFoundException(playeruuid);
	}


	
	@Override
	public void close() {
		
	}

	@Override
	public void refreshConfig() {
		internalFetcher = environment.center().config().getBoolean("fetchers.uuids.internal");
		ashconFetcher = environment.center().config().getBoolean("fetchers.uuids.ashcon");
		mojangFetcher = environment.center().config().getBoolean("fetchers.uuids.mojang");
	}
}
