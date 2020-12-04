/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.lang.reflect.Field;

import org.bukkit.command.CommandMap;

class CachingCommandMapHelper implements CommandMapHelper {

	private final CommandMapHelper delegate;
	
	private volatile Cache cache;
	
	CachingCommandMapHelper(CommandMapHelper delegate) {
		this.delegate = delegate;
	}
	
	private static class Cache {
		
		private final CommandMap commandMap;
		private final Field knownCommandsField;
		
		Cache(CommandMap commandMap, Field knownCommandsField) {
			this.commandMap = commandMap;
			this.knownCommandsField = knownCommandsField;
		}
		
	}
	
	private Cache getCache() {
		Cache cache = this.cache;
		if (cache == null) {
			synchronized (this) {
				cache = this.cache;
				if (cache == null) {
					CommandMap commandMap = delegate.getCommandMap();
					cache = new Cache(commandMap, delegate.getKnownCommandsField(commandMap));
					this.cache = cache;
				}
			}
		}
		return cache;
	}
	
	@Override
	public CommandMap getCommandMap() {
		return getCache().commandMap;
	}

	@Override
	public Field getKnownCommandsField(CommandMap commandMap) {
		return getCache().knownCommandsField;
	}

}
