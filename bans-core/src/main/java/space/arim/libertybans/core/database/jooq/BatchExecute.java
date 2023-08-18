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

import org.jooq.BatchBindStep;

import java.util.Objects;

public class BatchExecute<S> {

	private final BatchProvider batchProvider;
	private final BatchAttachment<S> batchAttachment;

	public BatchExecute(BatchProvider batchProvider, BatchAttachment<S> batchAttachment) {
		this.batchProvider = Objects.requireNonNull(batchProvider);
		this.batchAttachment = Objects.requireNonNull(batchAttachment);
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
	 * Function for attaching the source data to the batch object
	 *
	 * @param <S> the source data type
	 */
	public interface BatchAttachment<S> {

		/**
		 * Binds data
		 *
		 * @param batch the existing batch object
		 * @param data the data to bind
		 * @return the new batch object
		 */
		BatchBindStep attachData(BatchBindStep batch, S data);

	}

	/**
	 * Performs the batch execution
	 *
	 * @param source the source of the data
	 * @param maxBatchSize how many records to write in a single batch
	 */
	public void execute(Iterable<S> source, int maxBatchSize) {
		BatchBindStep batch = batchProvider.createBatch();
		for (S data : source) {
			batch = batchAttachment.attachData(batch, data);
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
