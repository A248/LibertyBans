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
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.config.Configs;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.UUIDUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class AdvancedBanImportSource implements ImportSource {

	private final ImportConfig config;
	private final ScopeManager scopeManager;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public AdvancedBanImportSource(Configs configs, ScopeManager scopeManager) {
		this(configs.getImportConfig(), scopeManager);
	}

	AdvancedBanImportSource(ImportConfig config, ScopeManager scopeManager) {
		this.config = config;
		this.scopeManager = scopeManager;
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {

		DatabaseStream databaseStream = new DatabaseStream(
				config.advancedBan().toConnectionSource(), config.retrievalSize());

		// Map active punishments first. Stream.of has guaranteed order
		return Stream.of(new RowMapper(true), new RowMapper(false))
				.flatMap(databaseStream::streamRows)
				// Filter punishments with the same details, in order to remove duplicates.
				.map(AdvancedBanUniquePunishmentDetails::new)
				.distinct()
				.map(AdvancedBanUniquePunishmentDetails::portablePunishment);
	}

	private class RowMapper implements SchemaRowMapper<PortablePunishment> {

		private final boolean active;

		RowMapper(boolean active) {
			this.active = active;
		}

		@Override
		public String selectStatement() {
			return "SELECT * FROM " + (active ? "Punishments" : "PunishmentHistory");
		}

		@Override
		public Optional<PortablePunishment> mapRow(ResultSet resultSet) throws SQLException {
			return mapType(resultSet).map((advancedBanType) -> {
				try {
					Integer id = resultSet.getInt("id");
					logger.trace("Mapping row with AdvancedBan punishment ID {}", id);
					return new PortablePunishment(
							id,
							mapKnownDetails(resultSet, advancedBanType),
							mapVictimInfo(resultSet, advancedBanType),
							mapOperatorInfo(resultSet),
							active);
				} catch (SQLException ex) {
					throw new ImportException(ex);
				}
			});
		}

		private Optional<AdvancedBanPunishmentType> mapType(ResultSet resultSet) throws SQLException {
			String punishmentType = getNonnullString(resultSet, "punishmentType");
			AdvancedBanPunishmentType advancedBanType;
			try {
				advancedBanType = AdvancedBanPunishmentType.valueOf(punishmentType);
			} catch (IllegalArgumentException typeNotRecognised) {

				// LibertyBans does not have AdvancedBan's NOTE
				// OR AdvancedBan has introduced another punishment type
				logger.info("Skipping punishment type {} which is not supported by LibertyBans", punishmentType);
				return Optional.empty();
			}
			return Optional.of(advancedBanType);
		}

		/*
		 * Nearly every column retrieval has to account for the possibility of NULL values,
		 * because AdvancedBan's schema lacks constraints.
		 */

		private PortablePunishment.KnownDetails mapKnownDetails(
				ResultSet resultSet, AdvancedBanPunishmentType advancedBanType) throws SQLException {
			// AdvancedBan's start and end times have milliseconds precision
			long startMillis = resultSet.getLong("start");
			if (startMillis == 0L) { // SQL NULL -> 0L
				throw nullColumn("start");
			}
			Instant start = Instant.ofEpochMilli(startMillis);

			long endMillis = resultSet.getLong("end");
			if (endMillis == 0L) { // SQL NULL -> 0L
				throw nullColumn("end");
			}
			// AdvancedBan uses -1 for permanent punishments
			Instant end;
			if (endMillis == -1L) {
				end = Punishment.PERMANENT_END_DATE;
			} else {
				end = Instant.ofEpochMilli(endMillis);
			}
			String reason = getNonnullString(resultSet, "reason");
			return new PortablePunishment.KnownDetails(
					advancedBanType.type(),
					reason,
					scopeManager.globalScope(), // AdvancedBan does not support scopes
					start, end);
		}

		private PortablePunishment.VictimInfo mapVictimInfo(
				ResultSet resultSet, AdvancedBanPunishmentType type) throws SQLException {
			// Despite the column name 'uuid', this can also be an IP address OR a username
			String victimId = getNonnullString(resultSet, "uuid");
			String name = getNonnullString(resultSet, "name");
			if (type.isIpAddressBased()) {
				NetworkAddress address;
				try {
					address = NetworkAddress.of(InetAddress.getByName(victimId));
				} catch (UnknownHostException ex) {
					throw new ImportException("Unable to parse AdvancedBan IP address", ex);
				}
				return new PortablePunishment.VictimInfo(null, name, address);
			}
			// AdvancedBan uses trimmed UUIDs, which are always of length 32
			if (victimId.length() == 32) {
				UUID uuid = UUIDUtil.fromShortString(victimId);
				return new PortablePunishment.VictimInfo(uuid, name, null);
			}
			// In this case the uuid and name columns should have the same value
			// Note well: The 'name' column is correctly cased, the 'uuid' column is not
			assert victimId.equalsIgnoreCase(name) : "Victim ID " + victimId + " should equal " + name;
			return new PortablePunishment.VictimInfo(null, name, null);
		}

		private PortablePunishment.OperatorInfo mapOperatorInfo(ResultSet resultSet) throws SQLException {
			String operatorName = getNonnullString(resultSet, "operator");
			if (operatorName.equals("CONSOLE")) {
				return PortablePunishment.OperatorInfo.createConsole();
			}
			return PortablePunishment.OperatorInfo.createUser(null, operatorName);
		}
	}

	private static String getNonnullString(ResultSet resultSet, String column) throws SQLException {
		String value = resultSet.getString(column);
		if (value == null) {
			throw nullColumn(column);
		}
		return value;
	}

	private static ThirdPartyCorruptDataException nullColumn(String column) {
		return new ThirdPartyCorruptDataException("Value of column " + column + " is null");
	}

	private enum AdvancedBanPunishmentType {
		BAN(PunishmentType.BAN),
		TEMP_BAN(PunishmentType.BAN),
		IP_BAN(PunishmentType.BAN),
		TEMP_IP_BAN(PunishmentType.BAN),
		MUTE(PunishmentType.MUTE),
		TEMP_MUTE(PunishmentType.MUTE),
		WARNING(PunishmentType.WARN),
		TEMP_WARNING(PunishmentType.WARN),
		KICK(PunishmentType.KICK)
		// NOTE(null); - purposefully omitted; see above
		;

		private final PunishmentType type;

		private AdvancedBanPunishmentType(PunishmentType type) {
			this.type = type;
		}

		PunishmentType type() {
			return type;
		}

		boolean isIpAddressBased() {
			return name().contains("IP_");
		}
	}

}
