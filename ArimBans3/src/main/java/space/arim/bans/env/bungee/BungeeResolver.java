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
package space.arim.bans.env.bungee;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import space.arim.bans.api.exception.FetcherException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.util.Web;
import space.arim.bans.env.Resolver;

public class BungeeResolver implements Resolver {
	
	private final BungeeEnv environment;
	
	private boolean ashconFetcher;
	private boolean mojangFetcher;
	
	private boolean ipStack;
	private String ipStackKey;
	private boolean freeGeoIp;
	
	public BungeeResolver(BungeeEnv environment) {
		this.environment = environment;
		refreshConfig();
	}
	
	private void handle(Exception ex) {
		if (ex instanceof MissingCacheException || ex instanceof FetcherException) {
			return;
		}
		// ProtocolException is a subclass of IOException
		if (ex instanceof IOException || ex instanceof ParseException) {
			environment.center().logError(ex);
			return;
		}
		ex.printStackTrace();
	}

	@Override
	public UUID uuidFromName(final String name) throws PlayerNotFoundException {
		Objects.requireNonNull(name, "name must not be null!");
		try {
			UUID uuid1 = environment.center().cache().getUUID(name);
			return uuid1;
		} catch (Exception ex) {
			handle(ex);
		}
		if (ashconFetcher) {
			try {
				UUID uuid3 = Web.ashconApi(name);
				environment.center().cache().update(uuid3, name, null);
				return uuid3;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		if (mojangFetcher) {
			try {
				UUID uuid4 = Web.mojangApi(name);
				environment.center().cache().update(uuid4, name, null);
				return uuid4;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		throw new PlayerNotFoundException(name);
	}

	@Override
	public String nameFromUUID(final UUID playeruuid) throws PlayerNotFoundException {
		Objects.requireNonNull(playeruuid, "uuid must not be null!");
		try {
			String name1 = environment.center().cache().getName(playeruuid);
			return name1;
		} catch (Exception ex) {
			handle(ex);
		}
		if (ashconFetcher) {
			try {
				String name3 = Web.ashconApi(playeruuid);
				environment.center().cache().update(playeruuid, name3, null);
				return name3;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		if (mojangFetcher) {
			try {
				String name4 = Web.mojangApi(playeruuid);
				environment.center().cache().update(playeruuid, name4, null);
				return name4;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		throw new PlayerNotFoundException(playeruuid);
	}
	
	@Override
	public JSONObject lookupIp(final String address) throws IllegalArgumentException, NoGeoIpException {
		if (ipStack) {
			try {
				return Web.ipStack(address, ipStackKey);
			} catch (Exception ex) {}
		}
		if (freeGeoIp) {
			try {
				return Web.freeGeoIp(address);
			} catch (Exception ex) {}
		}
		throw new NoGeoIpException(address);
	}
	
	@Override
	public void close() throws Exception {
		
	}

	@Override
	public void refreshConfig() {
		ashconFetcher = environment.center().config().getBoolean("fetchers.uuids.ashcon");
		mojangFetcher = environment.center().config().getBoolean("fetchers.uuids.mojang");
		ipStack = environment.center().config().getBoolean("fetchers.ips.ipstack.enabled");
		ipStackKey = environment.center().config().getString("fetchers.ips.ipstack.key");
		freeGeoIp = environment.center().config().getBoolean("fetchers.ips.freegeoip.enabled");
	}
}
