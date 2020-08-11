/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.configure.SingleKeyValueTransformer;
import space.arim.api.configure.ValueTransformer;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;

public class DatabaseManager implements Part {

	private final LibertyBansCore core;
	
	private volatile Database database;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public DatabaseManager(LibertyBansCore core) {
		this.core = core;
	}
	
	public Database getCurrentDatabase() {
		return database;
	}
	
	@Override
	public void startup() {
		this.database = new DatabaseSettings(core).create();
	}
	
	@Override
	public void restart() {
		Database database = this.database;
		getShutdownExecutor().execute(database::close);
		this.database = new DatabaseSettings(core).create();
	}

	@Override
	public void shutdown() {
		Database database = this.database;
		CompletableFuture.runAsync(() -> {
			database.close();
		}, getShutdownExecutor()).join();
	}
	
	private static Executor getShutdownExecutor() {
		return CompletableFuture.delayedExecutor(4, TimeUnit.SECONDS);
	}
	
	/*
	 * 
	 * Configuration
	 * 
	 */
	
	private static boolean isAtLeast(Object value, int amount) {
		return value instanceof Integer && ((Integer) value) >= amount;
	}
	
	public static List<ValueTransformer> createConfigTransformers() {
		var poolSizeTransformer = SingleKeyValueTransformer.createPredicate("connection-pool.size", (value) -> {
			if (!isAtLeast(value, 0)) {
				logger.warn("Bad connection pool size {}", value);
				return true;
			}
			return false;
		});
		var timeoutTransformer = SingleKeyValueTransformer.createPredicate("timeouts.connection-timeout-seconds", (value) -> {
			if (!isAtLeast(value, 1)) {
				logger.warn("Bad connection timeout setting {}", value);
				return false;
			}
			return true;
		});
		var lifetimeTransformer = SingleKeyValueTransformer.createPredicate("timeouts.max-lifetime-minutes", (value) -> {
			if (!isAtLeast(value, 1)) {
				logger.warn("Bad lifetime timeout setting {}", value);
				return false;
			}
			return true;
		});
		return List.of(poolSizeTransformer, timeoutTransformer, lifetimeTransformer);
	}
	
}
