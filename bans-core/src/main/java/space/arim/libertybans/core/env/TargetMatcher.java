/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
package space.arim.libertybans.core.env;

import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.NetworkAddress;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;

public interface TargetMatcher {

	boolean matches(UUID uuid, @Nullable InetAddress address);

	record UUIDs(Set<UUID> uuids) implements TargetMatcher {

		public UUIDs {
			uuids = Set.copyOf(uuids);
		}

		@Override
		public boolean matches(UUID uuid, @Nullable InetAddress address) {
			return uuids.contains(uuid);
		}
	}

	record Address(InetAddress address) implements TargetMatcher {

		public Address(NetworkAddress address) {
			this(address.toInetAddress());
		}

		@Override
		public boolean matches(UUID uuid, @Nullable InetAddress address) {
			return this.address.equals(address);
		}
	}

	record Combined(TargetMatcher matcher1, TargetMatcher matcher2) implements TargetMatcher {

		@Override
		public boolean matches(UUID uuid, @Nullable InetAddress address) {
			return matcher1.matches(uuid, address) || matcher2.matches(uuid, address);
		}
	}
}
