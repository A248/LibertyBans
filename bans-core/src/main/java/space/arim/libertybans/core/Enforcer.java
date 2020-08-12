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
package space.arim.libertybans.core;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentEnforcer;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.Configs.AddressStrictness;
import space.arim.libertybans.core.database.Database;
import space.arim.libertybans.core.env.TargetMatcher;

public class Enforcer implements PunishmentEnforcer {

	private final LibertyBansCore core;
	
	Enforcer(LibertyBansCore core) {
		this.core = core;
	}
	
	@Override
	public CentralisedFuture<?> enforce(Punishment punishment) {
		MiscUtil.validate(punishment);
		CentralisedFuture<SendableMessage> futureMessage = core.getFormatter().getPunishmentMessage(punishment);
		switch (punishment.getVictim().getType()) {
		case PLAYER:
			UUID uuid = ((PlayerVictim) punishment.getVictim()).getUUID();
			return futureMessage.thenAccept((msg) -> core.getEnvironment().getEnforcer().kickByUUID(uuid, msg));
		case ADDRESS:
			return futureMessage.thenCompose((message) -> enforceAddressPunishment(punishment, message));
		default:
			throw new IllegalStateException("Unknown victim type " + punishment.getVictim().getType());
		}
	}
	
	private CentralisedFuture<?> enforceAddressPunishment(Punishment punishment, SendableMessage message) {
		InetAddress address = ((AddressVictim) punishment.getVictim()).getAddress();
		CentralisedFuture<TargetMatcher> futureMatcher;
		AddressStrictness strictness = core.getConfigs().getAddressStrictness();
		switch (strictness) {
		case LENIENT:
			TargetMatcher matcher = new TargetMatcher(Set.of(), Set.of(address), message, shouldKick(punishment));
			futureMatcher = core.getFuturesFactory().completedFuture(matcher);
			break;
		case NORMAL:
			futureMatcher = matchAddressPunishmentNormal(address, punishment, message);
			break;
		case STRICT:
			futureMatcher = matchAddressPunishmentStrict(address, punishment, message);
			break;
		default:
			throw new IllegalStateException("Unknown address strictness " + strictness);
		}
		return futureMatcher.thenAccept(core.getEnvironment().getEnforcer()::enforceMatcher);
	}
	
	private static boolean shouldKick(Punishment punishment) {
		PunishmentType type = punishment.getType();
		switch (type) {
		case BAN:
		case KICK:
			return true;
		case MUTE:
		case WARN:
			return false;
		default:
			throw new IllegalStateException("Unknown punishment type " + type);
		}
	}

	private CentralisedFuture<TargetMatcher> matchAddressPunishmentNormal(InetAddress address, Punishment punishment,
			SendableMessage message) {
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Set<UUID> uuids = helper.jdbCaesar().query(
					"SELECT `uuid` FROM `libertybans_addresses` WHERE `address` = ?")
					.params(address)
					.setResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.onError(Set::of).execute();
			return new TargetMatcher(uuids, Set.of(), message, shouldKick(punishment));
		});
	}
	
	private CentralisedFuture<TargetMatcher> matchAddressPunishmentStrict(InetAddress address, Punishment punishment, SendableMessage message) {
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Set<UUID> uuids = helper.jdbCaesar().query(
					"SELECT `addrs`.`uuid` FROM `libertybans_addresses` `addrs` INNER JOIN `libertybans_address` `addrsAlso` "
					+ "ON `addrs`.`address` = `addrsAlso`.`address` WHERE `addrsAlso`.`uuid` = ?")
					.params(address)
					.setResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.onError(Set::of).execute();
			return new TargetMatcher(uuids, Set.of(), message, shouldKick(punishment));
		});
	}
	
}
