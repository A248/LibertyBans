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
package space.arim.libertybans.core.commands;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.config.MessagesConfig;

public abstract class AbstractSubCommandGroup implements SubCommandGroup {

	final Commands commands;
	/**
	 * Matching commands, lowercased
	 * 
	 */
	private final Set<String> matches;
	
	AbstractSubCommandGroup(Commands commands, Set<String> matches) {
		this.commands = commands;
		this.matches = Set.copyOf(matches);
	}
	
	AbstractSubCommandGroup(Commands commands, String...matches) {
		this(commands, Set.of(matches));
	}
	
	AbstractSubCommandGroup(Commands commands, Stream<String> matches) {
		this(commands, matches.collect(Collectors.toUnmodifiableSet()));
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
	
	<T> CentralisedFuture<T> completedFuture(T value) {
		return core().getFuturesFactory().completedFuture(value);
	}
	
	LibertyBansCore core() {
		return commands.core;
	}
	
	MainConfig config() {
		return core().getConfigs().getMainConfig();
	}
	
	MessagesConfig messages() {
		return core().getMessagesConfig();
	}
	
}
