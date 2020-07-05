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
				timedStartup();
				runState = RUNNING;
			}
		} finally {
			runStateLock.unlock();
		}
	}
	
	private void timedStartup() {
		infoMessage("Starting...");
		long startTime = System.nanoTime();
		startup0();
		long endTime = System.nanoTime();
		double millis = TimeUnit.MILLISECONDS.convert(startTime - endTime, TimeUnit.NANOSECONDS);
		infoMessage(String.format("Started up in %0.3f seconds", millis));
	}

	protected abstract void startup0();

	public boolean restart() {
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
			timedStartup();
			timedStop();
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
				timedStop();
				runState = IDLE;
			}
		} finally {
			runStateLock.unlock();
		}
	}
	
	private void timedStop() {
		infoMessage("Stopping...");
		long startTime = System.nanoTime();
		shutdown0();
		long endTime = System.nanoTime();
		double millis = TimeUnit.MILLISECONDS.convert(startTime - endTime, TimeUnit.NANOSECONDS);
		infoMessage(String.format("Shut down in %0.3f seconds", millis));
	}
	
	protected abstract void shutdown0();
	
	protected abstract void infoMessage(String message);
	
}
