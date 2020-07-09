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
package space.arim.libertybans.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.universal.util.ThisClass;
import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.api.util.sql.CloseMe;
import space.arim.api.util.sql.SqlBackend;

class DbHelper {
	
	private final LibertyBansCore core;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	DbHelper(LibertyBansCore core) {
		this.core = core;
	}

	SqlBackend getBackend() {
		return Objects.requireNonNull(getNullableBackend(), "Database not yet started");
	}
	
	SqlBackend getNullableBackend() {
		return core.getDatabase().getBackend();
	}
	
	Executor getExecutor() {
		return core.getDatabase().getExecutor();
	}
	
	CentralisedFuture<?> executeAsync(Runnable command) {
		return core.getFuturesFactory().runAsync(command, core.getDatabase().getExecutor());
	}
	
	<T> CentralisedFuture<T> selectAsync(Supplier<T> supplier) {
		return core.getFuturesFactory().supplyAsync(supplier, core.getDatabase().getExecutor());
	}
	
	CentralisedFuture<String> readResource(String resourceName) {
		return core.getFuturesFactory().supplyAsync(() -> {
			try (InputStream inputStream = getClass().getResourceAsStream("/sql/" + resourceName);
					ByteArrayOutputStream buf = new ByteArrayOutputStream()) {

				inputStream.transferTo(buf);
				return buf.toString(StandardCharsets.UTF_8);

			} catch (IOException ex) {
				logger.error("Failed to read internal resource {}", resourceName, ex);
			}
			return null;
		});
	}
	
	static class HsqldbCleanerRunnable implements Runnable {

		private final DbHelper helper;
		
		private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
		
		HsqldbCleanerRunnable(DbHelper helper) {
			this.helper = helper;
		}
		
		@Override
		public void run() {
			SqlBackend backend = helper.getNullableBackend();
			if (backend == null) {
				// cancelled but not stopped yet, or failed to stop
				logger.debug("HSQLDB cleaning task continues after shutdown");
				return;
			}
			try (CloseMe cm = backend.execute("CALL `libertybans_refresh`()")) {
				
			} catch (SQLException ex) {
				logger.warn("Punishments refresh task failed", ex);
			}
		}
	}
	
	static class ThreadFactoryImpl implements ThreadFactory {
		
		private final String prefix;
		private int threadId = 1;
		
		private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
		
		ThreadFactoryImpl(String prefix) {
			this.prefix = prefix;
		}
		
		private synchronized int nextId() {
			return threadId++;
		}
		
		@Override
		public Thread newThread(Runnable r) {
			String name = prefix + nextId();
			logger.debug("Spawning new thread {}", name);
			return new Thread(r, name);
		}
		
	}
	
}
