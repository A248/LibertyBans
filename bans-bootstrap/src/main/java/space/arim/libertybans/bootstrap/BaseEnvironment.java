/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import static space.arim.libertybans.bootstrap.RunState.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseEnvironment {

	private volatile RunState runState = IDLE;
	private final Lock runStateLock = new ReentrantLock();

	public RunState getRunState() {
		return runState;
	}
	
	public void startup() {
		RunState initialRunState = runState;
		if (initialRunState != IDLE) {
			System.out.println("[LibertyBans] Not conducting startup because run state is " + initialRunState);
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

	public boolean fullRestart() {
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
	
	public void shutdown() {
		RunState initialRunState = runState;
		if (initialRunState != RUNNING) {
			System.out.println("[LibertyBans] Not conducting shutdown because run state is " + initialRunState);
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
		infoMessage("Conducting " + point + "...");
		long startTime = System.nanoTime();
		try {
			switch (point) {
			case START:
				startup0();
				break;
			case RESTART:
				restart0();
				break;
			case STOP:
				shutdown0();
				break;
			default:
				throw new IllegalArgumentException("Unknown load point " + point);
			}
		} catch (LoadingException failure) {
			infoMessage("Conducting " + point + " failed: " + failure.getMessage());
			Throwable cause = failure.getCause();
			if (cause != null) {
				infoMessage("Extended failure cause:");
				cause.printStackTrace(System.err);
			}
			return false;
		}
		long endTime = System.nanoTime();
		double seconds = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) / 1_000D;
		infoMessage(String.format("Finished " + point + " in %.3f seconds", seconds));
		return true;
	}
	
	protected abstract void startup0();
	
	protected abstract void restart0();
	
	protected abstract void shutdown0();
	
	protected abstract void infoMessage(String message);
	
	private enum LoadPoint {
		START,
		RESTART,
		STOP
	}
	
}
