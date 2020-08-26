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
package space.arim.libertybans.core.env;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import space.arim.api.env.annote.PlatformPlayer;

public class TargetMatcher {

	private final Set<UUID> uuids;
	private final Set<InetAddress> addresses;
	private final Consumer<@PlatformPlayer Object> callback;
	
	public TargetMatcher(Set<UUID> uuids, Set<InetAddress> addresses, Consumer<@PlatformPlayer Object> callback) {
		this.uuids = uuids;
		this.addresses = addresses;
		this.callback = callback;
	}
	
	public Set<UUID> uuids() {
		return uuids;
	}
	
	public Set<InetAddress> addresses() {
		return addresses;
	}
	
	public Consumer<@PlatformPlayer Object> callback() {
		return callback;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uuids.hashCode();
		result = prime * result + addresses.hashCode();
		result = prime * result + System.identityHashCode(callback);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof TargetMatcher)) {
			return false;
		}
		TargetMatcher other = (TargetMatcher) object;
		return callback == other.callback && uuids.equals(other.uuids) && addresses.equals(other.addresses);
	}
	
}
