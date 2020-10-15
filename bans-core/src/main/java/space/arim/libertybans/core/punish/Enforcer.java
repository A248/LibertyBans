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
package space.arim.libertybans.core.punish;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.annote.PlatformPlayer;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.Database;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;
import space.arim.libertybans.core.selector.AddressStrictness;

class Enforcer extends EnforcementCenterMember {

	Enforcer(EnforcementCenter center) {
		super(center);
	}

	CentralisedFuture<?> enforce(Punishment punishment) {
		CentralisedFuture<SendableMessage> futureMessage = core().getFormatter().getPunishmentMessage(punishment);
		switch (punishment.getVictim().getType()) {
		case PLAYER:
			UUID uuid = ((PlayerVictim) punishment.getVictim()).getUUID();
			CentralisedFuture<?> enforceFuture = futureMessage.thenAccept((message) -> {
				core().getEnvironment().getEnforcer().doForPlayerIfOnline(uuid, enforcementCallback(punishment, message));
			});
			return enforceFuture;
		case ADDRESS:
			return futureMessage.thenCompose((message) -> enforceAddressPunishment(punishment, message));
		default:
			throw MiscUtil.unknownVictimType(punishment.getVictim().getType());
		}
	}
	
	private static boolean shouldKick(PunishmentType type) {
		switch (type) {
		case BAN:
		case KICK:
			return true;
		case MUTE:
		case WARN:
			return false;
		default:
			throw MiscUtil.unknownType(type);
		}
	}
	
	private Consumer<@PlatformPlayer Object> enforcementCallback(Punishment punishment, SendableMessage message) {
		PunishmentType type = punishment.getType();
		boolean shouldKick = shouldKick(type);
		return (playerObj) -> {

			PlatformHandle handle = core().getEnvironment().getPlatformHandle();
			if (shouldKick) {
				handle.disconnectUser(playerObj, message);
			} else {
				handle.sendMessage(playerObj, message);

				/*
				 * Mute enforcement must additionally take into account the mute cache
				 */
				if (type == PunishmentType.MUTE) {
					EnvEnforcer envEnforcer = core().getEnvironment().getEnforcer();
					UUID uuid = envEnforcer.getUniqueIdFor(playerObj);
					NetworkAddress address = NetworkAddress.of(envEnforcer.getAddressFor(playerObj));
					core().getMuteCacher().setCachedMute(uuid, address, punishment);
				}
			}
		};
	}
	
	private CentralisedFuture<?> enforceAddressPunishment(Punishment punishment, SendableMessage message) {
		NetworkAddress address = ((AddressVictim) punishment.getVictim()).getAddress();
		CentralisedFuture<TargetMatcher> futureMatcher;
		AddressStrictness strictness = core().getMainConfig().enforcement().addressStrictness();
		switch (strictness) {
		case LENIENT:
			Consumer<@PlatformPlayer Object> callback = enforcementCallback(punishment, message);
			TargetMatcher matcher = new TargetMatcher(Set.of(), Set.of(address.toInetAddress()), callback);
			futureMatcher = completedFuture(matcher);
			break;
		case NORMAL:
			futureMatcher = matchAddressPunishmentNormal(address, punishment, message);
			break;
		case STRICT:
			futureMatcher = matchAddressPunishmentStrict(address, punishment, message);
			break;
		default:
			throw MiscUtil.unknownAddressStrictness(strictness);
		}
		return futureMatcher.thenAccept(core().getEnvironment().getEnforcer()::enforceMatcher);
	}

	private CentralisedFuture<TargetMatcher> matchAddressPunishmentNormal(NetworkAddress address, Punishment punishment,
			SendableMessage message) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {
			Set<UUID> uuids = database.jdbCaesar().query(
					"SELECT `uuid` FROM `libertybans_addresses` WHERE `address` = ?")
					.params(address)
					.setResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.onError(Set::of).execute();
			return new TargetMatcher(uuids, Set.of(), enforcementCallback(punishment, message));
		});
	}
	
	private CentralisedFuture<TargetMatcher> matchAddressPunishmentStrict(NetworkAddress address, Punishment punishment,
			SendableMessage message) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {
			Set<UUID> uuids = database.jdbCaesar().query(
					"SELECT `addrs`.`uuid` FROM `libertybans_addresses` `addrs` INNER JOIN `libertybans_addresses` `addrsAlso` "
					+ "ON `addrs`.`address` = `addrsAlso`.`address` WHERE `addrsAlso`.`uuid` = ?")
					.params(address)
					.setResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.onError(Set::of).execute();
			return new TargetMatcher(uuids, Set.of(), enforcementCallback(punishment, message));
		});
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return core().getFuturesFactory().completedFuture(value);
	}
	
	CentralisedFuture<SendableMessage> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address) {
		core().getUUIDMaster().addCache(uuid, name);
		return core().getSelector().executeAndCheckConnection(uuid, name, address).thenCompose((ban) -> {
			if (ban == null) {
				return completedFuture(null);
			}
			return core().getFormatter().getPunishmentMessage(ban);
		});
	}
	
	CentralisedFuture<SendableMessage> checkChat(UUID uuid, NetworkAddress address, String command) {
		if (command != null && !blockForMuted(command)) {
			return completedFuture(null);
		}
		return core().getMuteCacher().getCachedMute(uuid, address).thenCompose((mute) -> {
			if (mute == null) {
				return completedFuture(null);
			}
			return core().getFormatter().getPunishmentMessage(mute);
		});
	}
	
	private boolean blockForMuted(String command) {
        String[] words = command.split(" ");
        // Handle commands with colons
        if (words[0].indexOf(':') != -1) {
            words[0] = words[0].split(":", 2)[1];
        }
        for (String muteCommand : core().getMainConfig().enforcement().muteCommands()) {
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
