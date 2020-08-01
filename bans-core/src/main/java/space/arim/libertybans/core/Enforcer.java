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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentEnforcer;
import space.arim.libertybans.core.env.OnlineTarget;

public class Enforcer implements PunishmentEnforcer {

	private final LibertyBansCore core;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	Enforcer(LibertyBansCore core) {
		this.core = core;
	}
	
	@Override
	public void enforce(Punishment punishment) {
		MiscUtil.validate(punishment);
		CentralisedFuture<SendableMessage> message = core.getFormatter().getPunishmentMessage(punishment);
		switch (punishment.getVictim().getType()) {
		case PLAYER:
			UUID uuid = ((PlayerVictim) punishment.getVictim()).getUUID();
			message.thenAccept((msg) -> core.getEnvironment().kickByUUID(uuid, msg));
			break;
		case ADDRESS:
			enforceAddressPunishment(punishment, message);
			break;
		default:
			throw new IllegalStateException("Unknown victim type " + punishment.getVictim().getType());
		}
	}
	
	private void removeAndKickIfMatching(Set<OnlineTarget> targets, byte[] address, SendableMessage message) {
		for (Iterator<OnlineTarget> it = targets.iterator(); it.hasNext(); ) {
			OnlineTarget target = it.next();
			if (Arrays.equals(target.getAddress(), address)) {
				target.kick(message);
				it.remove();
			}
		}
	}
	
	private void enforceAddressPunishment(Punishment punishment, CentralisedFuture<SendableMessage> futureMsg) {
		core.getEnvironment().getOnlineTargets().thenCombine(futureMsg, (targets, ignore) -> targets)
				.thenAcceptAsync((targets) -> {

			SendableMessage message = futureMsg.join(); // will be done by now, due to thenCombine
			byte[] address = ((AddressVictim) punishment.getVictim()).getAddress().getAddress();

			removeAndKickIfMatching(targets, address, message);
			if (targets.isEmpty()) {
				return;
			}

			StringBuilder queryBuilder = new StringBuilder(
					"SELECT `uuid`, `address` FROM `libertybans_addresses` WHERE `address` = ?");
			List<Object> args = new ArrayList<>();
			args.add(address);

			boolean foundFirst = false;
			for (OnlineTarget target : targets) {
				if (foundFirst) {
					queryBuilder.append(" OR ");
				} else {
					queryBuilder.append(" AND (");
					foundFirst = true;
				}
				queryBuilder.append("`uuid` = ?");
				args.add(target.getUniqueId());
			}
			if (foundFirst) {
				queryBuilder.append(')');
			}

			try (ResultSet rs = core.getDatabase().getBackend().select(queryBuilder.toString(), args.toArray())) {
				while (rs.next()) {
					removeAndKickIfMatching(targets, rs.getBytes("address"), message);
				}
			} catch (SQLException ex) {
				logger.error("Failed IP lookups for enforcing address-based punishment", ex);
			}
		}, core.getDatabase().getExecutor());
	}
	
}
