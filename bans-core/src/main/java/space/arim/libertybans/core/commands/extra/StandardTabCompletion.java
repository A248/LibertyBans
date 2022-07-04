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
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.Time;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static space.arim.libertybans.core.schema.tables.Names.NAMES;

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
		var config = configs.getMainConfig().commands().tabCompletion().offlinePlayerNames();
		if (config.enable()) {
			Duration retention = Duration.ofMinutes(config.retentionMinutes());
			Duration cacheRefresh = Duration.ofSeconds(config.cacheRefreshSeconds());
			AsyncLoadingCache<Boolean, Set<String>> nameCache = Caffeine.newBuilder()
					.refreshAfterWrite(cacheRefresh)
					.buildAsync((key, executor) -> {
						InternalDatabase database = dbProvider.get();
						Instant currentTimeMinusRetention = time.currentTimestamp().minus(retention);
						return database.query(SQLFunction.readOnly((context) -> {
							return Set.copyOf(context
									.select(NAMES.NAME)
									.from(NAMES)
									.where(NAMES.UPDATED.greaterThan(currentTimeMinusRetention))
									.fetchSet(NAMES.NAME));
						}));
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
	public Stream<String> completeOnlinePlayerNames(CmdSender sender) {
		if (configs.getMainConfig().commands().tabCompletion().useOnlyPlayersOnSameServer()) {
			return sender.getPlayerNamesOnSameServer();
		}
		return sender.getPlayerNames();
	}

	@Override
	public Stream<String> completeOfflinePlayerNames(CmdSender sender) {
		if (nameCache == null) {
			return completeOnlinePlayerNames(sender);
		}
		return nameCache.get(Boolean.TRUE).orTimeout(1L, TimeUnit.MILLISECONDS).join().stream();
	}

	@Override
	public Stream<String> completePunishmentDurations(CmdSender sender, PunishmentType type) {
		MainConfig config = configs.getMainConfig();
		var durationsConfig = config.commands().tabCompletion().punishmentDurations();
		if (durationsConfig.enable()) {

			// Only supply durations for which the sender has permission
			Predicate<ParsedDuration> permissionFilter;
			if (config.durationPermissions().enable()) {
				permissionFilter = (durPerm) -> durPerm.hasDurationPermission(sender, type);
			} else {
				permissionFilter = (durPerm) -> true;
			}
			return durationsConfig.durationsToSupply()
					.stream()
					.filter(permissionFilter)
					.map(ParsedDuration::value);
		}
		return Stream.empty();
	}

}
