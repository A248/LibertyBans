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

package space.arim.libertybans.core.uuid;

import java.util.UUID;

import jakarta.inject.Provider;

import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.database.InternalDatabase;

import static org.jooq.impl.DSL.lower;
import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.Names.NAMES;

class QueryingImpl {

	private final Provider<InternalDatabase> dbProvider;

	QueryingImpl(Provider<InternalDatabase> dbProvider) {
		this.dbProvider = dbProvider;
	}

	CentralisedFuture<UUID> resolve(String name) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.select(NAMES.UUID)
					.from(NAMES)
					.where(NAMES.LOWER_NAME.eq(lower(name)))
					.orderBy(NAMES.UPDATED.desc())
					.limit(1)
					.fetchOne(NAMES.UUID);
		}));
	}

	CentralisedFuture<String> resolve(UUID uuid) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.select(NAMES.NAME)
					.from(NAMES)
					.where(NAMES.UUID.eq(uuid))
					.orderBy(NAMES.UPDATED.desc())
					.limit(1)
					.fetchOne(NAMES.NAME);
		}));
	}

	/*
	 * Other lookups
	 */

	CentralisedFuture<NetworkAddress> resolveAddress(String name) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.select(ADDRESSES.ADDRESS)
					.from(ADDRESSES)
					.innerJoin(NAMES)
					.on(ADDRESSES.UUID.eq(NAMES.UUID))
					.where(NAMES.LOWER_NAME.eq(lower(name)))
					.orderBy(NAMES.UPDATED.desc(), ADDRESSES.UPDATED.desc())
					.limit(1)
					.fetchOne(ADDRESSES.ADDRESS);
		}));
	}

	CentralisedFuture<UUIDAndAddress> resolvePlayer(String name) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.select(ADDRESSES.UUID, ADDRESSES.ADDRESS)
					.from(NAMES)
					.innerJoin(ADDRESSES)
					.on(NAMES.UUID.eq(ADDRESSES.UUID))
					.where(NAMES.LOWER_NAME.eq(lower(name)))
					.orderBy(NAMES.UPDATED.desc(), ADDRESSES.UPDATED.desc())
					.limit(1)
					.fetchOne((record) -> {
						return new UUIDAndAddress(
								record.get(ADDRESSES.UUID),
								record.get(ADDRESSES.ADDRESS)
						);
					});
		}));
	}

	CentralisedFuture<NetworkAddress> resolveLastAddress(UUID uuid) {
		return dbProvider.get().query(SQLFunction.readOnly((context) -> {
			return context
					.select(ADDRESSES.ADDRESS)
					.from(ADDRESSES)
					.where(ADDRESSES.UUID.eq(uuid))
					.orderBy(ADDRESSES.UPDATED.desc())
					.limit(1)
					.fetchOne(ADDRESSES.ADDRESS);
		}));
	}

}
