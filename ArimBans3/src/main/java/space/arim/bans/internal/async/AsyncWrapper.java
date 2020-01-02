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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import space.arim.universal.registry.UniversalRegistry;

import space.arim.api.concurrent.AsyncExecutor;

public class AsyncWrapper implements AsyncMaster {

	private final AsyncExecutor executor;
	
	public AsyncWrapper(AsyncExecutor executor) {
		this.executor = executor;
	}
	
	@Override
	public void execute(Runnable command) {
		executor.execute(command);
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}

	@Override
	public boolean isClosed() {
		return executor != UniversalRegistry.get().getRegistration(AsyncExecutor.class);
	}
	
}
