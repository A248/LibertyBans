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

package space.arim.libertybans.core.commands.extra;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.Time;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Singleton
public final class StandardTabCompletion implements TabCompletion {

	private final Configs configs;
	private final Provider<InternalDatabase> dbProvider;
	private final Time time;

	private AsyncLoadingCache<Boolean, Set<String>> nameCache;

	@Inject
	public StandardTabCompletion(Configs configs, Provider<InternalDatabase> dbProvider, Time time) {
		this.configs = configs;
		this.dbProvider = dbProvider;
		this.time = time;
	}

	@Override
	public void startup() {
		TabCompletionConfig config = configs.getMainConfig().commands().tabCompletion();
		if (config.offlinePlayerNames()) {
			Duration retention = Duration.ofHours(config.offlinePlayerNamesRetentionHours());
			AsyncLoadingCache<Boolean, Set<String>> nameCache = Caffeine.newBuilder()
					.refreshAfterWrite(Duration.ofMinutes(10L))
					.buildAsync((key, executor) -> {
						InternalDatabase database = dbProvider.get();
						return database.selectAsync(() -> {
							long currentTimeMinusRetention = time.currentTimestamp().minus(retention).getEpochSecond();
							return Set.copyOf(database.jdbCaesar()
									.query("SELECT `name` FROM `libertybans_names`" +
											"WHERE `updated` > ?")
									.params(currentTimeMinusRetention)
									.setResult((resultSet) -> resultSet.getString("name"))
									.execute());
						});
					});
			// Load initial value
			nameCache.get(Boolean.TRUE).join();
			this.nameCache = nameCache;
		} else {
			nameCache = null;
		}
	}

	@Override
	public void restart() {
		startup();
	}

	@Override
	public void shutdown() {
		nameCache = null;
	}

	@Override
	public Stream<String> completeOfflinePlayerNames(CmdSender sender) {
		if (nameCache == null) {
			return sender.getPlayersOnSameServer();
		}
		return nameCache.get(Boolean.TRUE).orTimeout(1L, TimeUnit.MILLISECONDS).join().stream();
	}

}
