package space.arim.bans.env.bungee;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.json.simple.parser.ParseException;

import space.arim.bans.api.Tools;
import space.arim.bans.api.exception.FetcherException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.env.Resolver;

public class BungeeResolver implements Resolver {
	
	private BungeeEnv environment;
	
	private boolean ashconFetcher;
	private boolean mojangFetcher;
	
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
				UUID uuid3 = Tools.ashconApi(name);
				environment.center().cache().update(uuid3, name, null);
				return uuid3;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		if (mojangFetcher) {
			try {
				UUID uuid4 = Tools.mojangApi(name);
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
				String name3 = Tools.ashconApi(playeruuid);
				environment.center().cache().update(playeruuid, name3, null);
				return name3;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		if (mojangFetcher) {
			try {
				String name4 = Tools.mojangApi(playeruuid);
				environment.center().cache().update(playeruuid, name4, null);
				return name4;
			} catch (Exception ex) {
				handle(ex);
			}
		}
		throw new PlayerNotFoundException(playeruuid);
	}
	
	@Override
	public void close() throws Exception {
		
	}

	@Override
	public void refreshConfig() {
		ashconFetcher = environment.center().config().parseBoolean("fetchers.ashcon");
		mojangFetcher = environment.center().config().parseBoolean("fetchers.mojang");
	}
}
