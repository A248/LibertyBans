/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.database.jooq;

import org.jooq.Cursor;

import java.util.Objects;

public final class BatchTransfer<R extends org.jooq.Record> extends BatchExecute<R> {

	private final Cursor<R> cursor;

	public BatchTransfer(Cursor<R> cursor,
						 BatchProvider batchProvider,
						 BatchAttachment<R> batchAttachment) {
		super(batchProvider, batchAttachment);
		this.cursor = Objects.requireNonNull(cursor);
	}

	/**
	 * Performs the transfer
	 *
	 * @param maxBatchSize how many records to transfer in a single batch
	 */
	public void execute(int maxBatchSize) {
		try (cursor) {
			super.execute(cursor, maxBatchSize);
		}
	}
}
