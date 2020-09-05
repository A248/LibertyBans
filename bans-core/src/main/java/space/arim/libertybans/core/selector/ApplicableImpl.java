/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.selector;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.SecurePunishment;
import space.arim.libertybans.core.config.AddressStrictness;
import space.arim.libertybans.core.database.Database;

class ApplicableImpl extends SelectorImplGroup {

	ApplicableImpl(Selector selector) {
		super(selector);
	}
	
	private Map.Entry<String, Object[]> getApplicabilityQuery(UUID uuid, byte[] address, PunishmentType type) {
		String statement;
		Object[] args;

		AddressStrictness strictness = core().getConfigs().getAddressStrictness();
		switch (strictness) {
		case LENIENT:
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
					+ "FROM `libertybans_simple_" + type.getLowercaseNamePlural() + "` "
					+ "WHERE `type` = ? AND ((`victim_type` = 'PLAYER' AND `victim` = ?) OR (`victim_type` = 'ADDRESS' AND `victim` = ?))";
			args = new Object[] {type, uuid, address};
			break;
		case NORMAL:
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM `libertybans_applicable_" + type.getLowercaseNamePlural() + "` WHERE `type` = ? AND `uuid` = ?";
			args = new Object[] {type, uuid};
			break;
		case STRICT:
			statement = "SELECT `appl`.`id`, `appl`.`victim`, `appl`.`victim_type`, `appl`.`operator`, `appl`.`reason`, "
					+ "`appl`.`scope`, `appl`.`start`, `appl`.`end`, `appl`.`uuid`, `appl`.`address` FROM "
					+ "`libertybans_applicable_" + type.getLowercaseNamePlural() + "` `appl` INNER JOIN `libertybans_addresses` `addrs` "
					+ "ON `appl`.`address` = `addrs`.`address` WHERE `appl`.`type` = ? AND `appl`.`address` = ?";
			args = new Object[] {type, address};
			break;
		default:
			throw new IllegalStateException("Unknown AddressStrictness " + strictness);
		}
		return Map.entry(statement, args);
	}
	
	CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, byte[] address) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getApplicabilityQuery(uuid, address, PunishmentType.BAN);
			long currentTime = MiscUtil.currentTime();

			return database.jdbCaesar().transaction().transactor((querySource) -> {
				querySource.query(
						"INSERT INTO `libertybans_addresses` (`uuid`, `address`, `updated`) VALUES (?, ?, ?) "
								+ "ON DUPLICATE KEY UPDATE `updated` = ?")
						.params(uuid, address, currentTime, currentTime)
						.voidResult().execute();
				querySource.query(
						"INSERT INTO `libertybans_names` (`uuid`, `name`, `updated`) VALUES (?, ?, ?) "
						+ "ON DUPLICATE KEY UPDATE `updated` = ?")
						.params(uuid, name, currentTime, currentTime)
						.voidResult().execute();
				Punishment potentialBan = querySource.query(
						query.getKey())
						.params(query.getValue())
						.singleResult((resultSet) -> {
							return new SecurePunishment(resultSet.getInt("id"), PunishmentType.BAN,
									database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				return potentialBan;
			}).onRollback(() -> null).execute();
		});
	}
	
	private CentralisedFuture<Punishment> getApplicablePunishment0(UUID uuid, byte[] address, PunishmentType type) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getApplicabilityQuery(uuid, address, type);
			return database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.singleResult((resultSet) -> {
						return new SecurePunishment(resultSet.getInt("id"), type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(() -> null).execute();
		});
	}

	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, byte[] address, PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return core().getFuturesFactory().completedFuture(null);
		}
		return getApplicablePunishment0(uuid, address.clone(), type);
	}
	
	CentralisedFuture<Punishment> getApplicableMute(UUID uuid, byte[] address) {
		return getApplicablePunishment0(uuid, address, PunishmentType.MUTE);
	}

}
