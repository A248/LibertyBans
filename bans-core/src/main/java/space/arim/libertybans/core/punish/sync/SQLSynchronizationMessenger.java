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

package space.arim.libertybans.core.punish.sync;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;

import static space.arim.libertybans.core.schema.tables.Messages.MESSAGES;

@Singleton
public final class SQLSynchronizationMessenger implements SynchronizationMessenger {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> queryExecutor;
	private final Time time;

	private Instant lastTimestamp;

	@Inject
	public SQLSynchronizationMessenger(FactoryOfTheFuture futuresFactory,
									   Provider<QueryExecutor> queryExecutor, Time time) {
		this.futuresFactory = futuresFactory;
		this.queryExecutor = queryExecutor;
		this.time = time;
	}

	@Override
	public CentralisedFuture<Void> dispatch(byte[] message) {
		return queryExecutor.get().execute((context) -> {
			context
					.insertInto(MESSAGES)
					.columns(MESSAGES.MESSAGE, MESSAGES.TIME)
					.values(message, time.currentTimestamp())
					.execute();
		});
	}

	@Override
	public CentralisedFuture<byte[][]> poll() {
		Instant currentTime = time.currentTimestamp();
		if (lastTimestamp == null) {
			// The server has recently started up or LibertyBans has restarted
			lastTimestamp = currentTime;
			return futuresFactory.completedFuture(new byte[][] {});
		}
		Condition timeCondition = MESSAGES.TIME.lessOrEqual(currentTime).and(MESSAGES.TIME.greaterThan(lastTimestamp));
		var future = queryExecutor.get().query(SQLFunction.readOnly((context) -> {
			return context
					.select(MESSAGES.MESSAGE)
					.from(MESSAGES)
					.where(timeCondition)
					.orderBy(MESSAGES.TIME.asc())
					.fetchArray(MESSAGES.MESSAGE);
		}));
		lastTimestamp = currentTime;
		return future;
	}

	public void setInitialTimestamp() {
		lastTimestamp = time.currentTimestamp();
	}

	public void resetLastTimestamp() {
		lastTimestamp = null;
	}
}
