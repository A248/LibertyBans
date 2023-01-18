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

package space.arim.libertybans.core.commands.extra;

import jakarta.inject.Inject;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.UUID;

public class StandardArgumentParser implements ArgumentParser {

	private final FactoryOfTheFuture futuresFactory;
	private final Configs configs;
	private final UUIDManager uuidManager;
	
	@Inject
	public StandardArgumentParser(FactoryOfTheFuture futuresFactory, Configs configs, UUIDManager uuidManager) {
		this.futuresFactory = futuresFactory;
		this.configs = configs;
		this.uuidManager = uuidManager;
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	private MessagesConfig.All.NotFound notFound() {
		return configs.getMessagesConfig().all().notFound();
	}

	@Override
	public CentralisedFuture<UUID> parseOrLookupUUID(CmdSender sender, String targetArg) {
		return switch (targetArg.length()) {
		case 36 -> {
			UUID uuid;
			try {
				uuid = UUID.fromString(targetArg);
			} catch (IllegalArgumentException ex) {
				sender.sendMessage(notFound().uuid().replaceText("%TARGET%", targetArg));
				yield completedFuture(null);
			}
			yield completedFuture(uuid);
		}
		case 32 -> {
			long mostSigBits;
			long leastSigBits;
			try {
				mostSigBits = Long.parseUnsignedLong(targetArg.substring(0, 16), 16);
				leastSigBits = Long.parseUnsignedLong(targetArg.substring(16, 32), 16);
			} catch (NumberFormatException ex) {
				sender.sendMessage(notFound().uuid().replaceText("%TARGET%", targetArg));
				yield completedFuture(null);
			}
			yield completedFuture(new UUID(mostSigBits, leastSigBits));
		}
		default -> uuidManager.lookupUUID(targetArg).thenApply((uuid) -> {
			if (uuid.isEmpty()) {
				sender.sendMessage(notFound().player().replaceText("%TARGET%", targetArg));
				return null;
			}
			return uuid.get();
		});
		};
	}

	@Override
	public CentralisedFuture<Operator> parseOperator(CmdSender sender, String operatorArg) {
		if (ContainsCI.containsIgnoreCase(configs.getMessagesConfig().formatting().consoleArguments(), operatorArg)) {
			return completedFuture(ConsoleOperator.INSTANCE);
		}
		return parseOrLookupUUID(sender, operatorArg).thenApply((uuid) -> {
			return (uuid == null) ? null : PlayerOperator.of(uuid);
		});
	}

	@Override
	public CentralisedFuture<Victim> parseVictim(CmdSender sender, String targetArg, ParseVictim how) {
		NetworkAddress parsedAddress = AddressParser.parseIpv4(targetArg);
		if (parsedAddress != null) {
			return completedFuture(AddressVictim.of(parsedAddress));
		}
		Victim.VictimType preferredType = how.preferredType();
		return switch (preferredType) {
			case PLAYER -> parseOrLookupUUID(sender, targetArg).thenApply((uuid) -> {
				return (uuid == null) ? null : PlayerVictim.of(uuid);
			});
			case ADDRESS -> uuidManager.lookupAddress(targetArg).thenApply((address) -> {
				if (address == null) {
					sender.sendMessage(notFound().playerOrAddress().replaceText("%TARGET%", targetArg));
					return null;
				}
				return AddressVictim.of(address);
			});
			case COMPOSITE -> parsePlayer(sender, targetArg).thenApply((uuidAndAddress) -> {
				if (uuidAndAddress == null) {
					return null;
				}
				return CompositeVictim.of(uuidAndAddress.uuid(), uuidAndAddress.address());
			});
		};
	}

	@Override
	public CentralisedFuture<UUIDAndAddress> parsePlayer(CmdSender sender, String targetArg) {
		return  uuidManager.lookupPlayer(targetArg).thenApply((optUuidAndAddress) -> {
			if (optUuidAndAddress.isEmpty()) {
				sender.sendMessage(notFound().player().replaceText("%TARGET%", targetArg));
				return null;
			}
			return optUuidAndAddress.get();
		});
	}

}
