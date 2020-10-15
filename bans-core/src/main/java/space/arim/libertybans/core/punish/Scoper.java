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
package space.arim.libertybans.core.punish;

import java.util.Objects;

import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.manager.ScopeManager;

public class Scoper implements ScopeManager {

	public Scoper() {

	}
	
	@Override
	public ServerScope specificScope(String server) {
		return ScopeImpl.of(server);
	}

	@Override
	public ServerScope globalScope() {
		return ScopeImpl.GLOBAL;
	}
	
	@Override
	public ServerScope currentServerScope() {
		return ScopeImpl.GLOBAL; // TODO implement scopes
	}
	
	public String getServer(ServerScope scope) {
		if (!(scope instanceof ScopeImpl)) {
			throw new IllegalArgumentException("Foreign implementation of Scope: " + scope.getClass());
		}
		return ((ScopeImpl) scope).server;
	}
	
	static void checkScope(ServerScope scope) {
		if (scope == null) {
			throw new NullPointerException("scope");
		}
		if (!(scope instanceof ScopeImpl)) {
			throw new IllegalArgumentException("Foreign implementation of scope " + scope.getClass());
		}
	}
	
	private static class ScopeImpl implements ServerScope {
		
		final String server;
		
		static final ServerScope GLOBAL = new ScopeImpl(null);
		
		private ScopeImpl(String server) {
			this.server = server;
		}
		
		/**
		 * Gets a scope applying to a specific server
		 * 
		 * @param server the server name
		 * @return a scope applying to the server
		 */
		static ServerScope of(String server) {
			Objects.requireNonNull(server, "server");
			if (server.length() > 32) {
				throw new IllegalArgumentException("Server must be 32 or less chars");
			}
			return new ScopeImpl(server);
		}

		@Override
		public boolean appliesTo(String server) {
			return this.server == null || this.server.equals(server);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hashCode(server);
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof ScopeImpl)) {
				return false;
			}
			ScopeImpl other = (ScopeImpl) object;
			return Objects.equals(server, other.server);
		}

		@Override
		public String toString() {
			return (server == null) ? "global" : "server:" + server;
		}
		
	}

}
