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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import space.arim.libertybans.api.RunState;
import static space.arim.libertybans.api.RunState.*;

public abstract class BaseEnvironment {

	private volatile RunState runState = IDLE;
	private final Lock runStateLock = new ReentrantLock();

	public RunState getRunState() {
		return runState;
	}
	
	public void startup() {
		if (runState != IDLE) {
			return;
		}
		runStateLock.lock();
		try {
			if (runState == IDLE) {
				runState = LOADING;
				timedEvent(LoadPoint.START);
				runState = RUNNING;
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
			timedEvent(LoadPoint.RESTART);
			runState = RUNNING;
		} finally {
			runStateLock.unlock();
		}
		return true;
	}
	
	public void shutdown() {
		if (runState != RUNNING) {
			return;
		}
		runStateLock.lock();
		try {
			if (runState == RUNNING) {
				runState = LOADING;
				timedEvent(LoadPoint.STOP);
				runState = IDLE;
			}
		} finally {
			runStateLock.unlock();
		}
	}
	
	private void timedEvent(LoadPoint point) {
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
			return;
		}
		long endTime = System.nanoTime();
		double seconds = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) / 1_000D;
		infoMessage(String.format("Finished " + point + " in %.3f seconds", seconds));
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
