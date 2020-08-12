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

import space.arim.api.chat.SendableMessage;

public class TargetMatcher {

	private final Set<UUID> uuids;
	private final Set<InetAddress> addresses;
	private final SendableMessage message;
	private final boolean kick;
	
	public TargetMatcher(Set<UUID> uuids, Set<InetAddress> addresses, SendableMessage message, boolean kick) {
		this.uuids = uuids;
		this.addresses = addresses;
		this.message = message;
		this.kick = kick;
	}
	
	public Set<UUID> uuids() {
		return uuids;
	}
	
	public Set<InetAddress> addresses() {
		return addresses;
	}
	
	public SendableMessage message() {
		return message;
	}
	
	public boolean kick() {
		return kick;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uuids.hashCode();
		result = prime * result + addresses.hashCode();
		result = prime * result + message.hashCode();
		result = prime * result + (kick ? 1231 : 1237);
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
		return kick == other.kick && uuids.equals(other.uuids) && addresses.equals(other.addresses)
				&& message.equals(other.message);
	}
	
}
