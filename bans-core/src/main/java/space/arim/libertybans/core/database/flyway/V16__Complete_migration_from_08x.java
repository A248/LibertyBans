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

package space.arim.libertybans.core.database.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.BatchBindStep;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.jooq.OperatorBinding;
import space.arim.libertybans.core.database.sql.SequenceValue;
import space.arim.libertybans.core.database.sql.SerializedVictim;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.database.sql.VictimTableFields;
import space.arim.libertybans.core.schema.Sequences;
import space.arim.libertybans.core.schema.tables.Bans;
import space.arim.libertybans.core.schema.tables.History;
import space.arim.libertybans.core.schema.tables.Mutes;
import space.arim.libertybans.core.schema.tables.Warns;
import space.arim.libertybans.core.schema.tables.ZeroeightBans;
import space.arim.libertybans.core.schema.tables.ZeroeightHistory;
import space.arim.libertybans.core.schema.tables.ZeroeightMutes;
import space.arim.libertybans.core.schema.tables.ZeroeightWarns;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.UUIDUtil;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.jooq.impl.DSL.val;
import static org.jooq.impl.SQLDataType.BIGINT;
import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.VARBINARY;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.Names.NAMES;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;
import static space.arim.libertybans.core.schema.tables.Victims.VICTIMS;
import static space.arim.libertybans.core.schema.tables.ZeroeightAddresses.ZEROEIGHT_ADDRESSES;
import static space.arim.libertybans.core.schema.tables.ZeroeightHistory.ZEROEIGHT_HISTORY;
import static space.arim.libertybans.core.schema.tables.ZeroeightNames.ZEROEIGHT_NAMES;
import static space.arim.libertybans.core.schema.tables.ZeroeightPunishments.ZEROEIGHT_PUNISHMENTS;

public final class V16__Complete_migration_from_08x extends BaseJavaMigration {

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Override
	public void migrate(Context flywayContext) throws Exception {
		MigrationState migrationState = MigrationState.retrieveState(flywayContext);
		Connection connection = flywayContext.getConnection();
		DSLContext context = migrationState.createJooqContext(connection);

		if (new TableExists("libertybans_zeroeight_punishments").exists(context)) {
			completeMigration(context);
		}
	}

	private void completeMigration(DSLContext context) {
		logger.info("Completing migration of data from 0.8.x to 1.0.0");

		transferAllData(context);

		new Schema08x("zeroeight_", context).renameTables("zeroeight_postmigration_");

		logger.info("Completed migration of data from 0.8.x to 1.0.0");
	}

	private void transferAllData(DSLContext context) {
		// Move names and addresses
		transferData(
				context
						.select(ZEROEIGHT_NAMES.UUID, ZEROEIGHT_NAMES.NAME, ZEROEIGHT_NAMES.UPDATED)
						.from(ZEROEIGHT_NAMES)
						.fetchLazy(),
				() -> context.batch(context
						.insertInto(NAMES)
						.columns(NAMES.UUID, NAMES.NAME, NAMES.UPDATED)
						.values((UUID) null, null, null)
				),
				(batch, record) -> {
					UUID uuid = UUIDUtil.fromByteArray(record.value1());
					String name = record.value2();
					Instant updated = record.value3();
					return batch.bind(uuid, name, updated);
				}
		);
		transferData(
				context
						.select(ZEROEIGHT_ADDRESSES.UUID, ZEROEIGHT_ADDRESSES.ADDRESS, ZEROEIGHT_ADDRESSES.UPDATED)
						.from(ZEROEIGHT_ADDRESSES)
						.fetchLazy(),
				() -> context.batch(context
						.insertInto(ADDRESSES)
						.columns(ADDRESSES.UUID, ADDRESSES.ADDRESS, ADDRESSES.UPDATED)
						.values((UUID) null, null, null)
				),
				(batch, record) -> {
					UUID uuid = UUIDUtil.fromByteArray(record.value1());
					NetworkAddress address = record.value2();
					Instant updated = record.value3();
					return batch.bind(uuid, address, updated);
				}
		);
		// Copy punishment data
		transferData(
				context
						.select(
								ZEROEIGHT_PUNISHMENTS.ID, ZEROEIGHT_PUNISHMENTS.TYPE,
								ZEROEIGHT_PUNISHMENTS.OPERATOR, ZEROEIGHT_PUNISHMENTS.REASON,
								ZEROEIGHT_PUNISHMENTS.SCOPE, ZEROEIGHT_PUNISHMENTS.START, ZEROEIGHT_PUNISHMENTS.END
						)
						.from(ZEROEIGHT_PUNISHMENTS)
						.fetchLazy(),
				() -> context.batch(context
						.insertInto(PUNISHMENTS)
						.columns(PUNISHMENTS.ID, PUNISHMENTS.TYPE,
								PUNISHMENTS.OPERATOR, PUNISHMENTS.REASON,
								PUNISHMENTS.SCOPE, PUNISHMENTS.START, PUNISHMENTS.END)
						.values((Long) null, null, null, null, null, null, null)
				),
				(batch, record) -> {
					long id = record.value1();
					PunishmentType type = PunishmentType.valueOf(record.value2());
					Operator operator = new OperatorBinding().uuidToOperator(UUIDUtil.fromByteArray(record.value3()));
					String reason = record.value4();
					ServerScope scope = record.value5();
					Instant start = record.value6();
					Instant end = record.value7();
					return batch.bind(
							id, type,
							operator, reason,
							scope, start, end
					);
				}
		);
		// Install victims using new scheme - victim IDs
		context
				.select(ZEROEIGHT_HISTORY.VICTIM, ZEROEIGHT_HISTORY.VICTIM_TYPE)
				.from(ZEROEIGHT_HISTORY)
				.fetch()
				.forEach((record) -> {
					Victim victim = new Victim08x(record.value2(), record.value1()).deserialize();
					Integer existingVictimId = context
							.select(VICTIMS.ID)
							.from(VICTIMS)
							.where(new VictimCondition(new VictimTableFields()).matchesVictim(victim))
							.fetchOne(VICTIMS.ID);

					if (existingVictimId != null) {
						// Victim already inserted
						return;
					}
					SerializedVictim serializedVictim = new SerializedVictim(victim);
					context
							.insertInto(VICTIMS)
							.columns(VICTIMS.ID, VICTIMS.TYPE, VICTIMS.UUID, VICTIMS.ADDRESS)
							.values(
									new SequenceValue<>(Sequences.LIBERTYBANS_VICTIM_IDS).nextValue(context),
									val(victim.getType(), VICTIMS.TYPE),
									val(serializedVictim.uuid(), VICTIMS.UUID),
									val(serializedVictim.address(), VICTIMS.ADDRESS)
							)
							.execute();
				});
		// Transfer history table and active punishments
		Map<Table<? extends Record3<Integer, byte[], String>>, Table<? extends Record2<Long, Integer>>> punishmentTables;
		punishmentTables = Map.of(
				ZeroeightHistory.ZEROEIGHT_HISTORY, History.HISTORY,
				ZeroeightBans.ZEROEIGHT_BANS, Bans.BANS,
				ZeroeightMutes.ZEROEIGHT_MUTES, Mutes.MUTES,
				ZeroeightWarns.ZEROEIGHT_WARNS, Warns.WARNS
		);
		punishmentTables.forEach((tableFrom, tableTo) -> {
			transferData(
					context
							.select(
									tableFrom.field("id", INTEGER),
									tableFrom.field("victim", VARBINARY(16)),
									tableFrom.field("victim_type", VARCHAR(7))
							)
							.from(tableFrom)
							.fetchLazy(),
					() -> context.batch(context
							.insertInto(tableTo)
							.columns(tableTo.field("id", BIGINT), tableTo.field("victim", INTEGER))
							.values((Long) null, null)
					),
					(batch, record) -> {
						long punishmentId = record.value1();

						Victim victim = new Victim08x(record.value3(), record.value2()).deserialize();

						// The victim ID must exist, since every victim was mapped prior
						Integer victimId = context
								.select(VICTIMS.ID)
								.from(VICTIMS)
								.where(new VictimCondition(new VictimTableFields()).matchesVictim(victim))
								.fetchSingle(VICTIMS.ID);
						Objects.requireNonNull(victimId, "victimId");
						return batch.bind(punishmentId, victimId);
					}
			);
		});
	}

	private <R extends org.jooq.Record> void transferData(
			Cursor<R> cursor,
			Supplier<BatchBindStep> batchSupplier,
			BiFunction<BatchBindStep, R, BatchBindStep> batchAttachment) {

		int maxBatchSize = 1000;
		BatchBindStep batch = batchSupplier.get();
		try (cursor) {
			for (R record : cursor) {
				batch = batchAttachment.apply(batch, record);
				if (batch.size() == maxBatchSize) {
					batch.execute();
					batch = batchSupplier.get();
				}
			}
			if (batch.size() > 0) {
				batch.execute();
			}
		}
	}
}
