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

package space.arim.libertybans.core.addon.exempt.vault;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.impl.AbstractFactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.BaseCentralisedFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

final class ControllableFactoryOfTheFuture extends AbstractFactoryOfTheFuture {

	private final List<Runnable> asyncTasks = new ArrayList<>();
	private final List<Runnable> syncTasks = new ArrayList<>();

	void runAsyncTasks() {
		synchronized (asyncTasks) {
			asyncTasks.forEach(Runnable::run);
			asyncTasks.clear();
		}
	}

	void runSyncTasks() {
		synchronized (syncTasks) {
			syncTasks.forEach(Runnable::run);
			syncTasks.clear();
		}
	}

	@Override
	public void execute(Runnable command) {
		synchronized (asyncTasks) {
			asyncTasks.add(command);
		}
	}

	@Override
	public void executeSync(Runnable command) {
		synchronized (syncTasks) {
			syncTasks.add(command);
		}
	}

	@Override
	public <U> CentralisedFuture<U> newIncompleteFuture() {
		return new BaseCentralisedFuture<U>(this) {
			@Override
			public Executor defaultExecutor() {
				return ControllableFactoryOfTheFuture.this;
			}
		};
	}

	@Override
	public String toString() {
		return "ControllableFactoryOfTheFuture{" +
				"asyncTasks=" + asyncTasks +
				", syncTasks=" + syncTasks +
				'}';
	}

}
