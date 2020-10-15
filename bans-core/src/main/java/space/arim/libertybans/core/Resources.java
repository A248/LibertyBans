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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.bootstrap.ShutdownException;

class Resources implements Part {

	private final LibertyBansCore core;
	
	private FactoryOfTheFuture futuresFactory;
	private EnhancedExecutor enhancedExecutor;
	
	/**
	 * Awaits the execution of all independent asynchronous execution chains.
	 * Ensures all plugin operations can be shutdown.
	 */
	private final ExecutorService universalJoiner;
	
	Resources(LibertyBansCore core) {
		this.core = core;
		universalJoiner = Executors.newFixedThreadPool(1, core.newThreadFactory("Joiner"));
	}
	
	FactoryOfTheFuture getFuturesFactory() {
		return futuresFactory;
	}
	
	synchronized EnhancedExecutor getEnhancedExecutor() {
		if (enhancedExecutor == null) {
			enhancedExecutor = core.getEnvironment().getPlatformHandle().createEnhancedExecutor();
		}
		return enhancedExecutor;
	}
	
	void postFuture(CentralisedFuture<?> future) {
		universalJoiner.execute(future::join);
	}

	@Override
	public void startup() {
		futuresFactory = core.getEnvironment().getPlatformHandle().createFuturesFactory();
	}
	
	@Override
	public void restart() {}

	@Override
	public void shutdown() {
		universalJoiner.shutdown();
		/*
		 * On Bukkit, this prevents deadlocks.
		 * 
		 * By awaiting termination through the futures factory, the managed wait
		 * implementation breaks dependencies arising from tasks needing the main thread
		 * The main thread executes this shutdown() method, so if it is simply blocked,
		 * tasks depending on it cannot complete.
		 * 
		 * On any platform other than Bukkit, this is uninmportant.
		 */
		boolean termination = futuresFactory.supplyAsync(() -> {
			try {
				return universalJoiner.awaitTermination(6L, TimeUnit.SECONDS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				getLogger().warn("Failed to shutdown all chains of asynchronous execution (Interrupted)", ex);
				return true;
			}
		}).join();

		if (!termination) {
			getLogger().warn("Failed to shutdown all chains of asynchronous execution");
		}
		if (futuresFactory instanceof AutoCloseable) {
			try {
				((AutoCloseable) futuresFactory).close();
			} catch (Exception ex) {
				throw new ShutdownException("Cannot shutdown factory of the future", ex);
			}
		}
	}
	
	private Logger getLogger() {
		return LoggerFactory.getLogger(getClass());
	}
	
}
