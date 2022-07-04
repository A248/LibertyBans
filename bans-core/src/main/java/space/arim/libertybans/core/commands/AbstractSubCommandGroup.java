/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.commands.extra.ArgumentParser;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.events.AsyncEvent;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractSubCommandGroup implements SubCommandGroup {

	private final Dependencies dependencies;
	/**
	 * Matching commands, lowercased
	 * 
	 */
	private final Set<String> matches;

	private AbstractSubCommandGroup(Dependencies dependencies, Set<String> matches) {
		this.dependencies = dependencies;
		this.matches = Set.copyOf(matches);
	}

	protected AbstractSubCommandGroup(Dependencies dependencies, String...matches) {
		this(dependencies, Set.of(matches));
	}

	protected AbstractSubCommandGroup(Dependencies dependencies, Stream<String> matches) {
		this(dependencies, matches.collect(Collectors.toUnmodifiableSet()));
	}

	@Singleton
	public static class Dependencies {

		final Omnibus omnibus;
		final FactoryOfTheFuture futuresFactory;
		final Configs configs;
		final ArgumentParser argumentParser;

		@Inject
		public Dependencies(Omnibus omnibus, FactoryOfTheFuture futuresFactory,
							Configs configs, ArgumentParser argumentParser) {
			this.omnibus = omnibus;
			this.futuresFactory = futuresFactory;
			this.configs = configs;
			this.argumentParser = argumentParser;
		}

	}

	@Override
	public boolean matches(String arg) {
		return matches.contains(arg);
	}

	/*
	 * 
	 * Helper methods
	 * 
	 */

	ArgumentParser argumentParser() {
		return dependencies.argumentParser;
	}

	FactoryOfTheFuture futuresFactory() {
		return dependencies.futuresFactory;
	}

	protected <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory().completedFuture(value);
	}

	Configs configs() {
		return dependencies.configs;
	}

	protected MainConfig config() {
		return configs().getMainConfig();
	}

	protected MessagesConfig messages() {
		return configs().getMessagesConfig();
	}

	<E extends AsyncEvent> CompletionStage<E> fireWithTimeout(E event) {
		return dependencies.omnibus.getEventBus().fireAsyncEvent(event).orTimeout(10L, TimeUnit.SECONDS);
	}

}
