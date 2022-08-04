/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.it.test.database;

import jakarta.inject.Provider;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.it.util.RandomUtil.randomBytes;

@ExtendWith(InjectionInvocationContextProvider.class)
public class SchemaIntegrityIT {

	private final Provider<QueryExecutor> queryExecutor;
	private final Time time;

	public SchemaIntegrityIT(Provider<QueryExecutor> queryExecutor, Time time) {
		this.queryExecutor = queryExecutor;
		this.time = time;
	}

	private CentralisedFuture<Void> insertRawAddress(byte[] address) {
		return queryExecutor.get().execute((context) -> {
			context
					.insertInto(ADDRESSES)
					.columns(ADDRESSES.UUID, ADDRESSES.ADDRESS.coerce(byte[].class), ADDRESSES.UPDATED)
					.values(UUID.randomUUID(), address, time.currentTimestamp())
					.execute();
		});
	}

	@TestTemplate
	public void addressLengthConstraint(QueryExecutor queryExecutor, Time time) {
		assertDoesNotThrow(insertRawAddress(randomBytes(4))::join, "IPv4 address");
		assertDoesNotThrow(insertRawAddress(randomBytes(16))::join, "IPv6 address");
		assertThrows(CompletionException.class, insertRawAddress(randomBytes(3))::join, "Address too short");
		assertThrows(CompletionException.class, insertRawAddress(randomBytes(12))::join, "Address too long for IPv4");
		assertThrows(CompletionException.class, insertRawAddress(randomBytes(19))::join, "Address too long");
	}

}
