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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.Enaction;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ImportExecutor {

	private final FactoryOfTheFuture futuresFactory;
	private final ImportFunction importFunction;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	private final Time time;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public ImportExecutor(FactoryOfTheFuture futuresFactory, ImportFunction importFunction,
						  Provider<InternalDatabase> dbProvider, PunishmentCreator creator, Time time) {
		this.futuresFactory = futuresFactory;
		this.importFunction = importFunction;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.time = time;
	}

	public CentralisedFuture<ImportStatistics> performImport(ImportSource importSource) {
		Objects.requireNonNull(importSource, "importSource");

		CentralisedFuture<ImportStatistics> future = futuresFactory.newIncompleteFuture();
		new Thread(() -> {
			try {
				future.complete(runImport(importSource));
			} catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
		}, "LibertyBans-Importer").start();
		return future;
	}

	private ImportStatistics runImport(ImportSource importSource) {
		ImportStatistics statistics = new ImportStatistics();
		try (BatchOperationExecutor batchExecutor = new BatchOperationExecutor(dbProvider.get())) {

			ImportSink importSink = new ImportSink(batchExecutor, statistics);
			transferPunishments(importSource, importSink);
			transferExplicitNameAddressRecords(importSource, importSink);

			logger.info("Import completed successfully. {}", statistics);

		} catch (ImportException | SQLException ex) {
			logger.error(
					"Unable to complete import successfully. It is recommended to remove " +
							"the partially completed data, investigate the cause of failure, and " +
							"try again when you are sure the problem has been corrected.", ex);
			statistics.markFailed();
		}
		return statistics;
	}

	private void transferPunishments(ImportSource importSource, ImportSink importSink) {
		try (Stream<PortablePunishment> punishmentStream = importSource.sourcePunishments()) {
			punishmentStream.forEach(punishment -> {
				Optional<Enaction.OrderDetails> enactionOrder = importFunction.createOrder(punishment, importSink);
				if (enactionOrder.isEmpty()) {
					logger.info("Skipped imported punishment with ID {} applying to victim {}",
							punishment.foreignId(), punishment.victimInfo());
					return;
				}
				addEnaction(importSink, punishment, new Enaction(enactionOrder.get(), creator));
				addImplicitNameAddressRecord(importSink, punishment);
			});
		}
	}

	private void addEnaction(ImportSink importSink, PortablePunishment punishment, Enaction enaction) {
		Instant end = punishment.knownDetails().end();
		// Check whether active and non-expired
		if (punishment.active() &&
				(end.equals(Punishment.PERMANENT_END_DATE) || end.compareTo(time.currentTimestamp()) > 0)) {
			importSink.addActivePunishment(enaction);
		} else {
			importSink.addHistoricalPunishment(enaction);
		}
	}

	private void addImplicitNameAddressRecord(ImportSink importSink, PortablePunishment punishment) {
		Instant startTime = punishment.knownDetails().start();
		PortablePunishment.VictimInfo victimInfo = punishment.victimInfo();
		victimInfo.uuid().ifPresent((uuid) -> {
			importSink.addNameAddressRecord(new NameAddressRecord(
					uuid, victimInfo.name().orElse(null), victimInfo.address().orElse(null), startTime));
		});
		PortablePunishment.OperatorInfo operatorInfo = punishment.operatorInfo();
		Optional<UUID> operatorUuid = operatorInfo.uuid();
		if (!operatorInfo.console() && operatorUuid.isPresent()) {
			importSink.addNameAddressRecord(new NameAddressRecord(
					operatorUuid.get(), operatorInfo.name().orElse(null), null, startTime));
		}
	}

	private void transferExplicitNameAddressRecords(ImportSource importSource, ImportSink importSink) {
		try (Stream<NameAddressRecord> nameAddressHistoryStream = importSource.sourceNameAddressHistory()) {
			nameAddressHistoryStream.forEach(importSink::addNameAddressRecord);
		}
	}
}
