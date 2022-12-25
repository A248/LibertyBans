/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core;

import static space.arim.libertybans.bootstrap.RunState.FAILED;
import static space.arim.libertybans.bootstrap.RunState.IDLE;
import static space.arim.libertybans.bootstrap.RunState.LOADING;
import static space.arim.libertybans.bootstrap.RunState.RUNNING;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.LoadingException;
import space.arim.libertybans.bootstrap.RunState;

abstract class AbstractBaseFoundation implements BaseFoundation {

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	private volatile RunState runState = IDLE;
	private final Lock runStateLock = new ReentrantLock();

	@Override
	public final RunState getRunState() {
		return runState;
	}

	@Override
	public final void startup() {
		RunState initialRunState = runState;
		if (initialRunState != IDLE) {
			logger.info("Not conducting startup because run state is " + initialRunState);
			return;
		}
		runStateLock.lock();
		try {
			if (runState == IDLE) {
				runState = LOADING;
				if (timedEvent(LoadPoint.START)) {
					runState = RUNNING;
				} else {
					runState = FAILED;
				}
			}
		} finally {
			runStateLock.unlock();
		}
	}

	@Override
	public final boolean fullRestart() {
		if (runState != RUNNING) {
			return false;
		}
		if (!runStateLock.tryLock()) {
			return false;
		}
		try {
			if (runState != RUNNING) {
				return false;
			}
			runState = LOADING;
			if (timedEvent(LoadPoint.RESTART)) {
				runState = RUNNING;
			} else {
				runState = FAILED;
			}
		} finally {
			runStateLock.unlock();
		}
		return true;
	}

	@Override
	public final void shutdown() {
		RunState initialRunState = runState;
		if (initialRunState != RUNNING) {
			logger.info("Not conducting shutdown because run state is " + initialRunState);
			return;
		}
		runStateLock.lock();
		try {
			if (runState == RUNNING) {
				runState = LOADING;
				if (timedEvent(LoadPoint.STOP)) {
					runState = IDLE;
				} else {
					runState = FAILED;
				}
			}
		} finally {
			runStateLock.unlock();
		}
	}

	private boolean timedEvent(LoadPoint point) {
		logger.info("Conducting " + point + "...");
		long startTime = System.nanoTime();
		try {
			switch (point) {
			case START -> startup0();
			case RESTART -> restart0();
			case STOP -> shutdown0();
			default -> throw new IllegalArgumentException("Unknown load point " + point);
			}
		} catch (LoadingException failure) {
			logger.warn("Conducting " + point + " failed: " + failure.getMessage());
			Throwable cause = failure.getCause();
			if (cause != null) {
				logger.warn("Extended failure cause:", cause);
			}
			return false;
		}
		long endTime = System.nanoTime();
		long timeMillis = Duration.ofNanos(endTime - startTime).toMillis();
		logger.info(String.format("Finished " + point + " in %.3f seconds", timeMillis / 1_000D));
		return true;
	}

	abstract void startup0();

	abstract void restart0();

	abstract void shutdown0();

	private enum LoadPoint {
		START, RESTART, STOP;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT) + " phase";
		}
	}

}
