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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.inject.Provider;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

public class LateBindingFactoryOfTheFuture implements FactoryOfTheFuture {

	private final Provider<FactoryOfTheFuture> provider;

	public LateBindingFactoryOfTheFuture(Provider<FactoryOfTheFuture> provider) {
		this.provider = provider;
	}

	private FactoryOfTheFuture delegate() {
		return provider.get();
	}

	@Override
	public void execute(Runnable command) {
		delegate().execute(command);
	}

	@Override
	public void executeSync(Runnable command) {
		delegate().executeSync(command);
	}

	@Override
	public CentralisedFuture<?> runAsync(Runnable command) {
		return delegate().runAsync(command);
	}

	@Override
	public CentralisedFuture<?> runAsync(Runnable command, Executor executor) {
		return delegate().runAsync(command, executor);
	}

	@Override
	public CentralisedFuture<?> runSync(Runnable command) {
		return delegate().runSync(command);
	}

	@Override
	public <T> CentralisedFuture<T> supplyAsync(Supplier<T> supplier) {
		return delegate().supplyAsync(supplier);
	}

	@Override
	public <T> CentralisedFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
		return delegate().supplyAsync(supplier, executor);
	}

	@Override
	public <T> CentralisedFuture<T> supplySync(Supplier<T> supplier) {
		return delegate().supplySync(supplier);
	}

	@Override
	public <T> CentralisedFuture<T> completedFuture(T value) {
		return delegate().completedFuture(value);
	}

	@Override
	public <T> ReactionStage<T> completedStage(T value) {
		return delegate().completedStage(value);
	}

	@Override
	public <T> CentralisedFuture<T> failedFuture(Throwable ex) {
		return delegate().failedFuture(ex);
	}

	@Override
	public <T> ReactionStage<T> failedStage(Throwable ex) {
		return delegate().failedStage(ex);
	}

	@Override
	public <T> CentralisedFuture<T> newIncompleteFuture() {
		return delegate().newIncompleteFuture();
	}

	@Override
	public <T> CentralisedFuture<T> copyFuture(CompletableFuture<T> completableFuture) {
		return delegate().copyFuture(completableFuture);
	}

	@Override
	public <T> ReactionStage<T> copyStage(CompletionStage<T> completionStage) {
		return delegate().copyStage(completionStage);
	}

	@Override
	public CentralisedFuture<?> allOf(CentralisedFuture<?>... futures) {
		return delegate().allOf(futures);
	}

	@Override
	public <T> CentralisedFuture<?> allOf(Collection<? extends CentralisedFuture<T>> futures) {
		return delegate().allOf(futures);
	}
	
}
