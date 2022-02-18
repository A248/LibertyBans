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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.config.Configs;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class LiteBansImportSource implements ImportSource {

	private final ImportConfig config;
	private final ScopeManager scopeManager;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public LiteBansImportSource(Configs configs, ScopeManager scopeManager) {
		this(configs.getImportConfig(), scopeManager);
	}

	LiteBansImportSource(ImportConfig config, ScopeManager scopeManager) {
		this.config = config;
		this.scopeManager = scopeManager;
	}

	private DatabaseStream databaseStream() {
		return new DatabaseStream(
				config.litebans().toConnectionSource(), config.retrievalSize());
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {
		DatabaseStream databaseStream = databaseStream();
		return Arrays.stream(LiteBansTable.values())
				.map(PunishmentRowMapper::new)
				.flatMap(databaseStream::streamRows);
	}

	private class PunishmentRowMapper implements SchemaRowMapper<PortablePunishment> {

		private final LiteBansTable table;

		PunishmentRowMapper(LiteBansTable table) {
			this.table = table;
		}

		@Override
		public String selectStatement() {
			return "SELECT * FROM " + config.litebans().tablePrefix() + table;
		}

		@Override
		public Optional<PortablePunishment> mapRow(ResultSet resultSet) throws SQLException {
			Integer id = resultSet.getInt("id");
			if (resultSet.getBoolean("ipban_wildcard")) {
				logger.warn("Skipped LiteBans wildcard IP ban {} which is not supported by LibertyBans", id);
				return Optional.empty();
			}
			logger.trace("Mapping row from table {} with ID {}", table, id);
			Optional<PortablePunishment.VictimInfo> victimInfo = mapVictimInfo(resultSet);
			if (victimInfo.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new PortablePunishment(
					id,
					mapKnownDetails(resultSet),
					victimInfo.get(),
					mapOperatorInfo(resultSet),
					table != LiteBansTable.KICKS && resultSet.getBoolean("active")
			));
		}

		private PortablePunishment.KnownDetails mapKnownDetails(ResultSet resultSet) throws SQLException {
			String reason = resultSet.getString("reason");
			if (reason.length() > 256) {
				// LiteBans permits longer reasons
				reason = reason.substring(0, 256);
				logger.info("Trimmed reason of excessively long LiteBans punishment. New reason is {}", reason);
			}
			// LiteBans' start and end times have milliseconds precision
			Instant start = Instant.ofEpochMilli(resultSet.getLong("time"));

			long liteBansUntil = resultSet.getLong("until");
			// Sometimes LiteBans uses 0 for permanent punishments, sometimes -1
			Instant end;
			if (liteBansUntil == -1L || liteBansUntil == 0L) {
				end = Punishment.PERMANENT_END_DATE;
			} else {
				end = Instant.ofEpochMilli(liteBansUntil);
			}

			String liteBansScope = resultSet.getString("server_scope");
			ServerScope scope = (liteBansScope == null || liteBansScope.equals("*")) ?
					scopeManager.globalScope() : scopeManager.specificScope(liteBansScope);

			return new PortablePunishment.KnownDetails(table.type, reason, scope, start, end);
		}

		private Optional<PortablePunishment.VictimInfo> mapVictimInfo(ResultSet resultSet) throws SQLException {
			/*
			 * For user bans, the IP can be null, properly defined, or the magic string "#offline#".
			 * For some IP-bans, the uuid is "#offline#" and the IP is the name of the player.
			 * In other IP-bans, the uuid is well-defined, but the IP is "#offline#".
			 * Presumably some of this is to support banning players who have never joined,
			 * but in practice it is quite absurd.
			 */
			String uuidString = getNonnullString(resultSet, "uuid");
			if (uuidString.equals("#offline#")) {
				logger.warn("Skipping punishment where the LiteBans-recorded uuid is \"{}\"", uuidString);
				return Optional.empty();
			}
			UUID uuid = UUID.fromString(uuidString);
			boolean ipban = resultSet.getBoolean("ipban");
			if (!ipban) {
				return Optional.of(
						new PortablePunishment.VictimInfo(uuid, null, null, PlayerVictim.of(uuid)));
			}
			String ipString = resultSet.getString("ip");
			if (ipString == null || ipString.equals("#offline#") || ipString.equals("#")) {
				logger.warn("Skipping IP-ban where the LiteBans-recorded IP is \"{}\"", ipString);
				return Optional.empty();
			}
			NetworkAddress address;
			try {
				address = NetworkAddress.of(InetAddress.getByName(ipString));
			} catch (UnknownHostException ex) {
				throw new ImportException("Unable to parse LiteBans IP address", ex);
			}
			return Optional.of(
					new PortablePunishment.VictimInfo(uuid, null, address, AddressVictim.of(address)));
		}

		private PortablePunishment.OperatorInfo mapOperatorInfo(ResultSet resultSet) throws SQLException {
			String operatorId = getNonnullString(resultSet, "banned_by_uuid");
			if (operatorId.equals("CONSOLE")) {
				return PortablePunishment.OperatorInfo.createConsole();
			}
			if (operatorId.length() != 36) {
				// LiteBans occasionally records a name in the banned_by_uuid column
				logger.warn("Found operator uuid '{}' with incorrect length (should be 36 characters). " +
								"LibertyBans will rely on the name in banned_by_name instead.", operatorId);
				String operatorName = getNonnullString(resultSet, "banned_by_name");
				return PortablePunishment.OperatorInfo.createUser(null, operatorName);
			}
			return PortablePunishment.OperatorInfo.createUser(UUID.fromString(operatorId),
					resultSet.getString("banned_by_name")); // okay if banned_by_name is null
		}

	}

	private static String getNonnullString(ResultSet resultSet, String column) throws SQLException {
		String value = resultSet.getString(column);
		if (value == null) {
			throw new ThirdPartyCorruptDataException("Value of column " + column + " is null");
		}
		return value;
	}

	private enum LiteBansTable {

		BANS(PunishmentType.BAN),
		MUTES(PunishmentType.MUTE),
		WARNINGS(PunishmentType.WARN),
		KICKS(PunishmentType.KICK);

		final PunishmentType type;

		private LiteBansTable(PunishmentType type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	@Override
	public Stream<NameAddressRecord> sourceNameAddressHistory() {
		return databaseStream().streamRows(new HistoryRowMapper());
	}

	private class HistoryRowMapper implements SchemaRowMapper<NameAddressRecord> {

		@Override
		public String selectStatement() {
			return "SELECT uuid, name, ip, date FROM " + config.litebans().tablePrefix() + "history";
		}

		@Override
		public Optional<NameAddressRecord> mapRow(ResultSet resultSet) throws SQLException {
			String ipString = resultSet.getString("ip");
			// LiteBans uses # as the console's IP address
			if (ipString.equals("#")) {
				logger.debug("Skipping IP address {} in address history", ipString);
				return Optional.empty();
			}
			UUID uuid = UUID.fromString(resultSet.getString("uuid"));
			String username = resultSet.getString("name");
			Instant timeRecorded = resultSet.getTimestamp("date").toInstant();
			if (ipString.equals("#offline#") || ipString.equals("#undefined#")) {
				return Optional.of(new NameAddressRecord(uuid, username, null, timeRecorded));
			}
			NetworkAddress address;
			try {
				address = NetworkAddress.of(InetAddress.getByName(ipString));
			} catch (UnknownHostException ex) {
				throw new ImportException("Unable to parse LiteBans IP address", ex);
			}
			return Optional.of(new NameAddressRecord(uuid, username, address, timeRecorded));
		}
	}

}
