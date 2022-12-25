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
package space.arim.libertybans.core.service;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.bootstrap.ShutdownException;

@Singleton
public class StandardAsynchronicityManager implements AsynchronicityManager {

	private final Omnibus omnibus;
	private final PlatformHandle envHandle;

	/**
	 * Awaits the execution of all independent asynchronous execution chains.
	 * Ensures all plugin operations can be shutdown.
	 */
	private ExecutorService universalJoiner;
	private FactoryOfTheFuture futuresFactory;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public StandardAsynchronicityManager(Omnibus omnibus, PlatformHandle envHandle) {
		this.omnibus = omnibus;
		this.envHandle = envHandle;
	}

	@Override
	public FactoryOfTheFuture get() {
		FactoryOfTheFuture futuresFactory = this.futuresFactory;
		if (futuresFactory == null) {
			throw new IllegalStateException("Not initialized");
		}
		return futuresFactory;
	}

	@Override
	public void postFuture(CompletionStage<?> future) {
		Objects.requireNonNull(future);
		universalJoiner.execute(() -> {
			try {
				future.toCompletableFuture().join();
			} catch (CompletionException | CancellationException ex) {
				logger.error("Exception during miscellaneous asynchronous computation", ex);
			}
		});
	}

	@Override
	public void startup() {
		universalJoiner = Executors.newFixedThreadPool(1, SimpleThreadFactory.create("Joiner"));
		futuresFactory =  omnibus.getRegistry()
				.getProvider(FactoryOfTheFuture.class)
				.orElseGet(envHandle::createFuturesFactory);
	}

	@Override
	public void restart() {
		shutdown();
		startup();
	}

	@Override
	public void shutdown() {
		universalJoiner.shutdown();

		/*
		 * On Bukkit, this prevents deadlocks. On any other platform, this is
		 * unimportant.
		 *
		 * By awaiting termination through the futures factory, the managed wait
		 * implementation breaks dependencies arising from tasks needing the main
		 * thread. If the main thread is simply blocked, tasks depending on it cannot
		 * complete.
		 */
		boolean termination = futuresFactory.supplyAsync(() -> {
			try {
				return universalJoiner.awaitTermination(6L, TimeUnit.SECONDS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				logger.warn("Failed to shutdown all chains of asynchronous execution (Interrupted)", ex);
				return true;
			}
		}).join();

		if (!termination) {
			logger.warn("Failed to shutdown all chains of asynchronous execution");
		}
		if (futuresFactory instanceof AutoCloseable) {
			try {
				((AutoCloseable) futuresFactory).close();
			} catch (Exception ex) {
				throw new ShutdownException("Cannot shutdown factory of the future", ex);
			}
		}
	}

}
