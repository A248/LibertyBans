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
package space.arim.libertybans.core;

import java.util.Objects;

import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.ScopeManager;

public class Scoper implements ScopeManager {

	@Override
	public Scope specificScope(String server) {
		return ScopeImpl.of(server);
	}

	@Override
	public Scope globalScope() {
		return ScopeImpl.GLOBAL;
	}
	
	public String getServer(Scope scope) {
		if (!(scope instanceof ScopeImpl)) {
			throw new IllegalStateException("Foreign implementation of Scope: " + scope.getClass());
		}
		return ((ScopeImpl) scope).server;
	}
	
	public static class ScopeImpl implements Scope {
		
		final String server;
		
		static final Scope GLOBAL = new ScopeImpl(null);
		
		private ScopeImpl(String server) {
			this.server = server;
		}
		
		/**
		 * Gets a scope applying to a specific server
		 * 
		 * @param server the server name
		 * @return a scope applying to the server
		 */
		static Scope of(String server) {
			if (server.length() > 32) {
				throw new IllegalArgumentException("Server must be 32 or less chars");
			}
			return new ScopeImpl(Objects.requireNonNull(server, "Server must not be null"));
		}

		@Override
		public boolean appliesTo(String server) {
			return this.server == null || this.server.equals(server);
		}
		
		@Override
		public String toString() {
			return server;
		}
		
	}

}
