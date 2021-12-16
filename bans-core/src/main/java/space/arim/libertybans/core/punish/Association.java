/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

package space.arim.libertybans.core.punish;

import org.jooq.DSLContext;
import space.arim.libertybans.api.NetworkAddress;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.Names.NAMES;

public final class Association {

	private final UUID uuid;
	private final DSLContext context;

	public Association(UUID uuid, DSLContext context) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.context = Objects.requireNonNull(context, "context");
	}

	public void associateCurrentName(String name, Instant currentTime) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(currentTime, "currentTime");
		context
				.insertInto(NAMES)
				.columns(NAMES.UUID, NAMES.NAME, NAMES.UPDATED)
				.values(uuid, name, currentTime)
				.onConflict(NAMES.UUID, NAMES.NAME)
				.doUpdate()
				.set(NAMES.UPDATED, currentTime)
				.execute();
	}

	public void associatePastName(String name, Instant pastTime) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(pastTime, "pastTime");
		context
				.insertInto(NAMES)
				.columns(NAMES.UUID, NAMES.NAME, NAMES.UPDATED)
				.values(uuid, name, pastTime)
				.onConflict(NAMES.UUID, NAMES.NAME)
				.doNothing()
				.execute();
	}

	public void associateCurrentAddress(NetworkAddress address, Instant currentTime) {
		Objects.requireNonNull(address, "address");
		Objects.requireNonNull(currentTime, "currentTime");
		context
				.insertInto(ADDRESSES)
				.columns(ADDRESSES.UUID, ADDRESSES.ADDRESS, ADDRESSES.UPDATED)
				.values(uuid, address, currentTime)
				.onConflict(ADDRESSES.UUID, ADDRESSES.ADDRESS)
				.doUpdate()
				.set(ADDRESSES.UPDATED, currentTime)
				.execute();
	}

	public void associatePastAddress(NetworkAddress address, Instant pastTime) {
		Objects.requireNonNull(address, "address");
		Objects.requireNonNull(pastTime, "pastTime");
		context
				.insertInto(ADDRESSES)
				.columns(ADDRESSES.UUID, ADDRESSES.ADDRESS, ADDRESSES.UPDATED)
				.values(uuid, address, pastTime)
				.onConflict(ADDRESSES.UUID, ADDRESSES.ADDRESS)
				.doNothing()
				.execute();
	}

	@Override
	public String toString() {
		return "Association{" +
				"uuid=" + uuid +
				", context=" + context +
				'}';
	}
}
