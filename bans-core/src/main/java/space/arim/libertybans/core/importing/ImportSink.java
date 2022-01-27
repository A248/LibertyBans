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

package space.arim.libertybans.core.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.punish.Association;
import space.arim.libertybans.core.punish.Enaction;
import space.arim.omnibus.util.ThisClass;

import java.time.Instant;

class ImportSink {

	private final BatchOperationExecutor batchExecutor;
	private final ImportStatistics statistics;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	ImportSink(BatchOperationExecutor batchExecutor, ImportStatistics statistics) {
		this.batchExecutor = batchExecutor;
		this.statistics = statistics;
	}

	void addActivePunishment(Enaction enaction) {
		addPunishment(enaction, true);
		statistics.transferredActive();
	}

	void addHistoricalPunishment(Enaction enaction) {
		addPunishment(enaction, false);
		statistics.transferredHistorical();
	}

	private void addPunishment(Enaction enaction, boolean active) {
		batchExecutor.runOperation((context, transaction) -> {
			if (active) {
				Punishment enacted = transaction.executeNested(enaction::enactActive);
				if (enacted == null) {
					logger.warn(
							"There is a conflicting active punishment: {}. For example two bans for the same user. " +
									"This is harmless in most cases. The punishment will be skipped.",
							enaction.orderDetails());
				}
			} else {
				enaction.enactHistorical(context);
			}
		});
	}

	void addNameAddressRecord(NameAddressRecord nameAddressRecord) {
		batchExecutor.runOperation((context, transaction) -> {
			Association association = new Association(nameAddressRecord.uuid(), context);
			Instant timeRecorded = nameAddressRecord.timeRecorded();
			nameAddressRecord.name().ifPresent((name) -> association.associatePastName(name, timeRecorded));
			nameAddressRecord.address().ifPresent((address) -> association.associatePastAddress(address, timeRecorded));
		});
		statistics.transferredNameAddressRecord();
	}

}
