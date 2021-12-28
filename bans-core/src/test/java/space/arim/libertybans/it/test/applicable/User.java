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

package space.arim.libertybans.it.test.applicable;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.Objects;
import java.util.UUID;

final class User {

	private final UUID uuid;
	private final NetworkAddress address;

	User(UUID uuid, NetworkAddress address) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
	}

	static User randomUser() {
		return new User(UUID.randomUUID(), RandomUtil.randomAddress());
	}

	UUID uuid() {
		return uuid;
	}

	NetworkAddress address() {
		return address;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + address.hashCode();
		result = prime * result + uuid.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof User)) {
			return false;
		}
		User other = (User) object;
		return uuid.equals(other.uuid) && address.equals(other.address);
	}

	@Override
	public String toString() {
		return "User [uuid=" + uuid + ", address=" + address + "]";
	}

}
