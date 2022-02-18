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

package space.arim.libertybans.core.importing;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.DatabaseConstants;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.DatabaseResult;
import space.arim.libertybans.core.database.DatabaseSettings;
import space.arim.libertybans.core.database.StandardDatabase;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.jooq.BatchTransfer;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static space.arim.libertybans.core.schema.tables.Messages.MESSAGES;
import static space.arim.libertybans.core.schema.tables.Revision.REVISION;

public final class SelfImportProcess {

	private final Path folder;
	private final Configs configs;
	private final DatabaseManager databaseManager;
	private final Provider<QueryExecutor> queryExecutor;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public SelfImportProcess(@Named("folder") Path folder, Configs configs,
							 DatabaseManager databaseManager, Provider<QueryExecutor> queryExecutor) {
		this.folder = folder;
		this.configs = configs;
		this.databaseManager = databaseManager;
		this.queryExecutor = queryExecutor;
	}

	public CentralisedFuture<Void> transferAllData() {
		return transferAllData(folder);
	}

	public CentralisedFuture<Void> transferAllData(Path folder) {
		logger.info("Beginning self-import process");

		return queryExecutor.get().execute((target) -> {
			ImportConfig importConfig = configs.getImportConfig();
			DatabaseResult dbResult = new DatabaseSettings(folder, databaseManager).create(importConfig.self());

			try (StandardDatabase database = dbResult.database()) {

				if (!dbResult.success()) {
					logger.warn("Failed to connect to import source");
					return;
				}
				database.execute((source) -> {
					new SelfImport(source, target, importConfig.retrievalSize()).runTransfer();
				}).join();
			}
			logger.info("Completed self-import process");
		});
	}

	private static final class SelfImport {

		private final DSLContext source;
		private final DSLContext target;
		private final int maxBatchSize;

		private SelfImport(DSLContext source, DSLContext target, int maxBatchSize) {
			this.source = source;
			this.target = target;
			this.maxBatchSize = maxBatchSize;
		}

		private void runTransfer() {
			for (Table<?> table : DatabaseConstants.allTables(DatabaseConstants.TableOrder.REFERENTS_FIRST)) {
				if (table.equals(REVISION) || table.equals(MESSAGES)) {
					continue;
				}
				logger.info("Beginning transfer of data from table: {}", table.getName());
				transferTable(table);
				logger.info("Finished transfer of data from table: {}", table.getName());
			}
		}

		private <R extends org.jooq.Record> void transferTable(Table<R> table) {

			// Table fields
			Field<?>[] fields;
			{
				// Exclude generated columns from transferred fields
				Set<String> forbiddenNames = Set.of("lower_name");

				List<Field<?>> fieldList = new ArrayList<>(Arrays.asList(table.newRecord().fields()));
				fieldList.removeIf((field) -> forbiddenNames.contains(field.getName()));
				fields = fieldList.toArray(Field[]::new);
				/*
				Note: JOOQ's OSS edition currently does not have the "read-only" column feature,
				which applies to generated columns. However, the commercial editions have this
				feature. If the feature ever becomes part of the OSS edition, switch to it here.
				 */
			}

			// Dummy null values for use with the batch API
			Object[] emptyValues = new Object[fields.length];

			new BatchTransfer<>(
					source.selectFrom(table).fetchLazy(),
					() -> target.batch(target
							.insertInto(table)
							.columns(fields)
							.values(emptyValues)
					),
					(batch, record) -> {
						Object[] bindValues = emptyValues.clone();
						for (int n = 0; n < fields.length; n++) {
							bindValues[n] = record.get(fields[n]);
						}
						return batch.bind(bindValues);
					}
			).transferData(maxBatchSize);
		}
	}
}
