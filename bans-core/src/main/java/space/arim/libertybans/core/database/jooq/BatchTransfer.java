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

package space.arim.libertybans.core.database.jooq;

import org.jooq.BatchBindStep;
import org.jooq.Cursor;

public final class BatchTransfer<R extends org.jooq.Record> {

	private final Cursor<R> cursor;
	private final BatchProvider batchProvider;
	private final BatchAttachment<R> batchAttachment;

	public BatchTransfer(Cursor<R> cursor,
						 BatchProvider batchProvider,
						 BatchAttachment<R> batchAttachment) {
		this.cursor = cursor;
		this.batchProvider = batchProvider;
		this.batchAttachment = batchAttachment;
	}

	/**
	 * Provides the batch object for the table into which the data will be inserted
	 */
	public interface BatchProvider {

		/**
		 * Creates the batch object
		 *
		 * @return a batch object for the target table
		 */
		BatchBindStep createBatch();
	}

	/**
	 * Function for attaching the cursor data to the batch object
	 *
	 * @param <R> the record type
	 */
	public interface BatchAttachment<R extends org.jooq.Record> {

		/**
		 * Binds data
		 * 
		 * @param batch the existing batch object
		 * @param record the data to bind
		 * @return the new batch object
		 */
		BatchBindStep attachData(BatchBindStep batch, R record);

	}

	/**
	 * Performs the transfer
	 *
	 * @param maxBatchSize how many records to transfer in a single batch
	 */
	public void transferData(int maxBatchSize) {
		BatchBindStep batch = batchProvider.createBatch();
		try (cursor) {
			for (R record : cursor) {
				batch = batchAttachment.attachData(batch, record);
				if (batch.size() == maxBatchSize) {
					batch.execute();
					batch = batchProvider.createBatch();
				}
			}
			if (batch.size() > 0) {
				batch.execute();
			}
		}
	}
}
