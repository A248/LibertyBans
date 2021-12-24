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

package space.arim.libertybans.core.env;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class AdditionalUUIDTargetMatcher<P> implements TargetMatcher<P> {

	private final UUID uuid;
	private final TargetMatcher<P> delegate;

	public AdditionalUUIDTargetMatcher(UUID uuid, TargetMatcher<P> delegate) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public boolean matches(UUID uuid, InetAddress address) {
		return this.uuid.equals(uuid) || delegate.matches(uuid, address);
	}

	@Override
	public Consumer<P> callback() {
		return delegate.callback();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AdditionalUUIDTargetMatcher<?> that = (AdditionalUUIDTargetMatcher<?>) o;
		return uuid.equals(that.uuid) && delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		int result = uuid.hashCode();
		result = 31 * result + delegate.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "AdditionalUUIDTargetMatcher{" +
				"uuid=" + uuid +
				", delegate=" + delegate +
				'}';
	}
}
