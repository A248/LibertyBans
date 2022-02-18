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
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.config.Configs;
import space.arim.omnibus.util.UUIDUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class BanManagerImportSource implements ImportSource {

	private final ImportConfig config;
	private final ScopeManager scopeManager;

	@Inject
	public BanManagerImportSource(Configs configs, ScopeManager scopeManager) {
		this(configs.getImportConfig(), scopeManager);
	}

	BanManagerImportSource(ImportConfig config, ScopeManager scopeManager) {
		this.config = config;
		this.scopeManager = scopeManager;
	}

	private DatabaseStream databaseStream() {
		return new DatabaseStream(
				config.banManager().toConnectionSource(), config.retrievalSize());
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {
		ImportConfig.BanManagerSettings banManagerConf = config.banManager();

		UUID consoleUuid = new BanManagerConsoleUUID(
				banManagerConf.toConnectionSource(),
				banManagerConf.tablePrefix()
		).retrieveConsoleUUID();

		DatabaseStream databaseStream = databaseStream();
		return Stream.of(BanManagerTable.values())
				.map((table) -> new RowMapper(table, consoleUuid))
				.flatMap(databaseStream::streamRows);
	}

	/**
	 * Mapper for BanManager. Unlike other import sources, BanManager has a robust database schem
	 * which maintains integrity, so we do not need null checks on NONNULL columns.
	 *
	 */
	private final class RowMapper implements SchemaRowMapper<PortablePunishment> {

		private final BanManagerTable table;
		private final UUID consoleUuid;

		private RowMapper(BanManagerTable table, UUID consoleUuid) {
			this.table = table;
			this.consoleUuid = consoleUuid;
		}

		@Override
		public String selectStatement() {
			String tableName = table.tableName(config.banManager().tablePrefix());
			return "SELECT * FROM \"" + tableName + '"';
		}

		@Override
		public Optional<PortablePunishment> mapRow(ResultSet resultSet) throws SQLException {
			int id = resultSet.getInt("id");
			return Optional.of(new PortablePunishment(
					id,
					mapKnownDetails(resultSet),
					PortablePunishment.VictimInfo.simpleVictim(table.victimKind.mapVictim(resultSet)),
					mapOperator(resultSet),
					table.active && table.type != PunishmentType.KICK
			));
		}

		private PortablePunishment.KnownDetails mapKnownDetails(ResultSet resultSet) throws SQLException {

			Instant start = Instant.ofEpochSecond(resultSet.getLong(table.active ? "created" : "pastCreated"));
			Instant end;
			if (table.type == PunishmentType.KICK) {
				end = Punishment.PERMANENT_END_DATE;
			} else {
				long expired = resultSet.getLong(table.active ? "expires" : "expired");
				end = (expired == 0L) ? Punishment.PERMANENT_END_DATE : Instant.ofEpochSecond(expired);
			}
			return new PortablePunishment.KnownDetails(
					table.type, resultSet.getString("reason"), scopeManager.globalScope(), start, end
			);
		}

		private PortablePunishment.OperatorInfo mapOperator(ResultSet resultSet) throws SQLException {
			byte[] actorId = resultSet.getBytes(table.active ? "actor_id" : "pastActor_id");

			UUID actorUuid = UUIDUtil.fromByteArray(actorId);
			if (actorUuid.equals(consoleUuid)) {
				return PortablePunishment.OperatorInfo.createConsole();
			}
			return PortablePunishment.OperatorInfo.createUser(actorUuid, null);
		}
	}

	@Override
	public Stream<NameAddressRecord> sourceNameAddressHistory() {
		return databaseStream().streamRows(new PlayersMapper());
	}

	private class PlayersMapper implements SchemaRowMapper<NameAddressRecord> {

		@Override
		public String selectStatement() {
			return "SELECT * FROM \"" + config.banManager().tablePrefix() + "players\" ORDER BY \"lastSeen\" ASC OFFSET 1";
		}

		@Override
		public Optional<NameAddressRecord> mapRow(ResultSet resultSet) throws SQLException {
			return Optional.of(new NameAddressRecord(
					UUIDUtil.fromByteArray(resultSet.getBytes("id")),
					resultSet.getString("name"),
					NetworkAddress.of(resultSet.getBytes("ip")),
					Instant.ofEpochSecond(resultSet.getLong("lastSeen"))
			));
		}
	}
}
