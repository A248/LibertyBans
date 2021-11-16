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

package space.arim.libertybans.core.alts;

import space.arim.libertybans.api.NetworkAddress;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class KnownAccount {

	private final UUID uuid;
	private final String username;
	private final NetworkAddress address;
	private final Instant updated;

	public KnownAccount(UUID uuid, String username, NetworkAddress address, Instant updated) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.username = Objects.requireNonNull(username, "username");
		this.address = Objects.requireNonNull(address, "address");
		this.updated = Objects.requireNonNull(updated, "updated");
	}

	public UUID uuid() {
		return uuid;
	}

	public String username() {
		return username;
	}

	public NetworkAddress address() {
		return address;
	}

	public Instant updated() {
		return updated;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KnownAccount that = (KnownAccount) o;
		return uuid.equals(that.uuid) && username.equals(that.username) && address.equals(that.address) && updated.equals(that.updated);
	}

	@Override
	public int hashCode() {
		int result = uuid.hashCode();
		result = 31 * result + username.hashCode();
		result = 31 * result + address.hashCode();
		result = 31 * result + updated.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "KnownAccount{" +
				"uuid=" + uuid +
				", username='" + username + '\'' +
				", address=" + address +
				", updated=" + updated +
				'}';
	}
}
