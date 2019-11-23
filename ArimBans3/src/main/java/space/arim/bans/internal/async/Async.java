/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Async implements AsyncMaster {
	private final ExecutorService threads;
	public Async() {
		refreshConfig();
		threads = Executors.newCachedThreadPool();
	}
	
	@Override
	public void execute(Runnable command) {
		threads.execute(command);
	}
	
	@Override
	public boolean isClosed() {
		return (threads != null) ? threads.isShutdown() : true;
	}
	
	private void shutdown() throws InterruptedException {
		threads.shutdown();
		threads.awaitTermination(20L, TimeUnit.SECONDS);
	}
	
	@Override
	public void close() throws InterruptedException {
		if (!isClosed()) {
			shutdown();
		}
	}

	@Override
	public void refreshConfig() {
		
	}
	
}
