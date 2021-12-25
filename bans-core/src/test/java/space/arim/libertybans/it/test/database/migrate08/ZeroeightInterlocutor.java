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

package space.arim.libertybans.it.test.database.migrate08;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.TableImpl;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.DatabaseConstants;
import space.arim.libertybans.core.database.jooq.OperatorBinding;
import space.arim.libertybans.core.importing.NameAddressRecord;
import space.arim.libertybans.core.schema.Sequences;
import space.arim.omnibus.util.UUIDUtil;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.jooq.impl.DSL.table;
import static space.arim.libertybans.core.database.DatabaseConstants.LIBERTYBANS_08X_FLYWAY_TABLE;
import static space.arim.libertybans.core.schema.tables.SchemaHistory.SCHEMA_HISTORY;
import static space.arim.libertybans.core.schema.tables.ZeroeightAddresses.ZEROEIGHT_ADDRESSES;
import static space.arim.libertybans.core.schema.tables.ZeroeightBans.ZEROEIGHT_BANS;
import static space.arim.libertybans.core.schema.tables.ZeroeightHistory.ZEROEIGHT_HISTORY;
import static space.arim.libertybans.core.schema.tables.ZeroeightMutes.ZEROEIGHT_MUTES;
import static space.arim.libertybans.core.schema.tables.ZeroeightNames.ZEROEIGHT_NAMES;
import static space.arim.libertybans.core.schema.tables.ZeroeightPunishments.ZEROEIGHT_PUNISHMENTS;
import static space.arim.libertybans.core.schema.tables.ZeroeightWarns.ZEROEIGHT_WARNS;

final class ZeroeightInterlocutor {

	private final DSLContext context;

	ZeroeightInterlocutor(DSLContext context) {
		this.context = context;
	}

	private void dropAllTables() {
		SQLDialect dialect = context.family();
		// Drop views
		for (Table<?> table : DatabaseConstants.allViews()) {
			context.dropView(table).execute();
		}
		// Drop tables
		for (Table<?> table : DatabaseConstants.allTables()) {
			context.dropTable(table).execute();
		}
		context.dropTable(SCHEMA_HISTORY).execute();
		// Drop sequences
		if (dialect == SQLDialect.MYSQL) {
			context.dropTable(table(Sequences.LIBERTYBANS_PUNISHMENT_IDS.getName())).execute();
			context.dropTable(table(Sequences.LIBERTYBANS_VICTIM_IDS.getName())).execute();
		} else {
			context.dropSequence(Sequences.LIBERTYBANS_PUNISHMENT_IDS).execute();
			context.dropSequence(Sequences.LIBERTYBANS_VICTIM_IDS).execute();
		}
	}

	void prepareTables(DataSource dataSource) throws SQLException {
		// Delete existing tables
		dropAllTables();
		// Create 0.8.x tables using Flyway
		//noinspection deprecation
		Flyway
				.configure(getClass().getClassLoader())
				.table(LIBERTYBANS_08X_FLYWAY_TABLE)
				.placeholders(Map.of(
						"zeroeighttableprefix", "libertybans_"
				))
				.locations("classpath:extra-database-migrations/zeroeight")
				// This will need to be modified when Flyway 9 removes this deprecated method
				.ignoreFutureMigrations(false)
				.validateMigrationNaming(true)
				.dataSource(dataSource)
				.baselineOnMigrate(true).baselineVersion("0")
				.load()
				.migrate();
	}

	private byte[] serializeVictim(Victim victim) {
		return switch (victim.getType()) {
			case PLAYER -> UUIDUtil.toByteArray(((PlayerVictim) victim).getUUID());
			case ADDRESS -> ((AddressVictim) victim).getAddress().getRawAddress();
			default -> throw new UnsupportedOperationException("Victim type " + victim.getType());
		};
	}

	void insertUser(NameAddressRecord nameAddressRecord) {
		Instant time = nameAddressRecord.timeRecorded();
		UUID uuid = nameAddressRecord.uuid();
		String name = nameAddressRecord.name().orElseThrow(AssertionError::new);
		NetworkAddress address = nameAddressRecord.address().orElseThrow(AssertionError::new);
		context
				.insertInto(ZEROEIGHT_NAMES)
				.columns(
						ZEROEIGHT_NAMES.UUID, ZEROEIGHT_NAMES.NAME, ZEROEIGHT_NAMES.UPDATED
				)
				.values(UUIDUtil.toByteArray(uuid), name, time)
				.onConflict(ZEROEIGHT_NAMES.UUID, ZEROEIGHT_NAMES.NAME)
				.doUpdate()
				.set(ZEROEIGHT_NAMES.UPDATED, time)
				.execute();
		context
				.insertInto(ZEROEIGHT_ADDRESSES)
				.columns(
						ZEROEIGHT_ADDRESSES.UUID, ZEROEIGHT_ADDRESSES.ADDRESS, ZEROEIGHT_ADDRESSES.UPDATED
				).values(UUIDUtil.toByteArray(uuid), address, time)
				.onConflict(ZEROEIGHT_ADDRESSES.UUID, ZEROEIGHT_ADDRESSES.ADDRESS)
				.doUpdate()
				.set(ZEROEIGHT_ADDRESSES.UPDATED, time)
				.execute();
	}

	void insertPunishment(Punishment punishment, boolean active) {
		int id = (int) punishment.getIdentifier();
		byte[] operator = UUIDUtil.toByteArray(new OperatorBinding().operatorToUuid(punishment.getOperator()));
		context
				.insertInto(ZEROEIGHT_PUNISHMENTS)
				.columns(
						ZEROEIGHT_PUNISHMENTS.ID, ZEROEIGHT_PUNISHMENTS.TYPE,
						ZEROEIGHT_PUNISHMENTS.OPERATOR, ZEROEIGHT_PUNISHMENTS.REASON,
						ZEROEIGHT_PUNISHMENTS.SCOPE, ZEROEIGHT_PUNISHMENTS.START, ZEROEIGHT_PUNISHMENTS.END
				)
				.values(
						id, punishment.getType().name(),
						operator, punishment.getReason(),
						punishment.getScope(), punishment.getStartDate(), punishment.getEndDate()
				)
				.execute();
		Victim victim = punishment.getVictim();
		byte[] victimData = serializeVictim(victim);
		String victimType = victim.getType().name();
		context
				.insertInto(ZEROEIGHT_HISTORY)
				.columns(ZEROEIGHT_HISTORY.ID, ZEROEIGHT_HISTORY.VICTIM, ZEROEIGHT_HISTORY.VICTIM_TYPE)
				.values(id, victimData, victimType)
				.execute();
		TableImpl<? extends Record3<Integer, byte[], String>> dataTable = switch (punishment.getType()) {
			case BAN -> ZEROEIGHT_BANS;
			case MUTE -> ZEROEIGHT_MUTES;
			case WARN -> ZEROEIGHT_WARNS;
			case KICK -> null;
		};
		if (!active || dataTable == null) {
			return;
		}
		var fields = dataTable.newRecord();
		context
				.insertInto(dataTable)
				.columns(fields.field1(), fields.field2(), fields.field3())
				.values(id, victimData, victimType)
				.execute();

	}
}
