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

import java.util.Set;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.AddressVictim.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentEnforcer;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.Configs.AddressStrictness;
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
		NetworkAddress address = ((AddressVictim) punishment.getVictim()).getAddress();
		CentralisedFuture<TargetMatcher> futureMatcher;
		AddressStrictness strictness = core.getConfigs().getAddressStrictness();
		switch (strictness) {
		case LENIENT:
			TargetMatcher matcher = new TargetMatcher(Set.of(), Set.of(address.toInetAddress()), message, shouldKick(punishment));
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

	private CentralisedFuture<TargetMatcher> matchAddressPunishmentNormal(NetworkAddress address, Punishment punishment,
			SendableMessage message) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			Set<UUID> uuids = database.jdbCaesar().query(
					"SELECT `uuid` FROM `libertybans_addresses` WHERE `address` = ?")
					.params(address)
					.setResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.onError(Set::of).execute();
			return new TargetMatcher(uuids, Set.of(), message, shouldKick(punishment));
		});
	}
	
	private CentralisedFuture<TargetMatcher> matchAddressPunishmentStrict(NetworkAddress address, Punishment punishment, SendableMessage message) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			Set<UUID> uuids = database.jdbCaesar().query(
					"SELECT `addrs`.`uuid` FROM `libertybans_addresses` `addrs` INNER JOIN `libertybans_address` `addrsAlso` "
					+ "ON `addrs`.`address` = `addrsAlso`.`address` WHERE `addrsAlso`.`uuid` = ?")
					.params(address)
					.setResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.onError(Set::of).execute();
			return new TargetMatcher(uuids, Set.of(), message, shouldKick(punishment));
		});
	}
	
	/**
	 * Enforces an incoming connection, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * Adds the UUID and name to the local fast cache, queries for an applicable ban, and formats the
	 * ban reason as the punishment message.
	 * 
	 * @param uuid the player's UUID
	 * @param name the player's name
	 * @param address the player's network address
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	public CentralisedFuture<SendableMessage> executeAndCheckConnection(UUID uuid, String name, byte[] address) {
		core.getUUIDMaster().addCache(uuid, name);
		return core.getSelector().executeAndCheckConnection(uuid, name, address).thenCompose((ban) -> {
			if (ban == null) {
				return core.getFuturesFactory().completedFuture(null);
			}
			return core.getFormatter().getPunishmentMessage(ban);
		});
	}
	
	/**
	 * Enforces a chat message or executed command, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * If this corresponds to an executed command, the configured commands whose access to muted players to block
	 * are taken into account.
	 * 
	 * @param uuid the player's UUID
	 * @param address the player's network address
	 * @param command the command executed, or {@code null} if this is a chat message
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	public CentralisedFuture<SendableMessage> checkChat(UUID uuid, byte[] address, String command) {
		if (command != null && !blockForMuted(command)) {
			return core.getFuturesFactory().completedFuture(null);
		}
		return core.getSelector().getCachedMute(uuid, address).thenCompose((mute) -> {
			if (mute == null) {
				return core.getFuturesFactory().completedFuture(null);
			}
			return core.getFormatter().getPunishmentMessage(mute);
		});
	}
	
	private boolean blockForMuted(String command) {
        String[] words = command.split(" ");
        // Handle commands with colons
        if (words[0].indexOf(':') != -1) {
            words[0] = words[0].split(":", 2)[1];
        }
        for (String muteCommand : core.getConfigs().getConfig().getStringList("enforcement.mute-commands")) {
            if (muteCommandMatches(words, muteCommand)) {
                return true;
            }
        }
        return false;
	}
	
	private static boolean muteCommandMatches(String[] commandWords, String muteCommand) {
        // Basic equality check
        if (commandWords[0].equalsIgnoreCase(muteCommand)) {
            return true;
        }
        // Advanced equality check
        // Essentially a case-insensitive "startsWith" for arrays
        if (muteCommand.indexOf(' ') != -1) {
            String[] muteCommandWords = muteCommand.split(" ");
            if (muteCommandWords.length > commandWords.length) {
                return false;
            }
            for (int n = 0; n < muteCommandWords.length; n++) {
                if (!muteCommandWords[n].equalsIgnoreCase(commandWords[n])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
	
}
