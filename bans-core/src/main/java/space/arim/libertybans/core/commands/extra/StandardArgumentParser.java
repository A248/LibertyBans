/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class StandardArgumentParser implements ArgumentParser {

	private final FactoryOfTheFuture futuresFactory;
	private final Configs configs;
	private final InternalScopeManager scopeManager;
	private final PunishmentFormatter formatter;
	private final UUIDManager uuidManager;

	@Inject
	public StandardArgumentParser(FactoryOfTheFuture futuresFactory, Configs configs, InternalScopeManager scopeManager,
								  PunishmentFormatter formatter, UUIDManager uuidManager) {
		this.futuresFactory = futuresFactory;
		this.configs = configs;
		this.scopeManager = scopeManager;
		this.formatter = formatter;
		this.uuidManager = uuidManager;
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	private MessagesConfig.All all() {
		return configs.getMessagesConfig().all();
	}

	private MessagesConfig.All.NotFound notFound() {
		return all().notFound();
	}

	private <R> CentralisedFuture<@Nullable R> parseUUIDIfPossible(CmdSender sender, String targetArg,
																   Function<UUID, CentralisedFuture<R>> ifUuid,
																   Supplier<CentralisedFuture<R>> otherwise) {
		return switch (targetArg.length()) {
			case 36 -> {
				UUID uuid;
				try {
					uuid = UUID.fromString(targetArg);
				} catch (IllegalArgumentException ex) {
					sender.sendMessage(notFound().uuid().replaceText("%TARGET%", targetArg));
					yield completedFuture(null);
				}
				yield ifUuid.apply(uuid);
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
				yield ifUuid.apply(new UUID(mostSigBits, leastSigBits));
			}
			default -> otherwise.get();
		};
	}

	@Override
	public CentralisedFuture<@Nullable UUID> parseOrLookupUUID(CmdSender sender, String targetArg) {
		return parseUUIDIfPossible(sender, targetArg, this::completedFuture,
				() -> uuidManager.lookupUUID(targetArg).thenApply((uuid) -> {
					if (uuid.isEmpty()) {
						sender.sendMessage(notFound().player().replaceText("%TARGET%", targetArg));
						return null;
					}
					return uuid.get();
				}));
	}

	@Override
	public CentralisedFuture<@Nullable Operator> parseOperator(CmdSender sender, String operatorArg) {
		if (ContainsCI.containsIgnoreCase(configs.getMessagesConfig().formatting().consoleArguments(), operatorArg)) {
			return completedFuture(ConsoleOperator.INSTANCE);
		}
		return parseOrLookupUUID(sender, operatorArg).thenApply((uuid) -> {
			return (uuid == null) ? null : PlayerOperator.of(uuid);
		});
	}

	@Override
	public CentralisedFuture<@Nullable Victim> parseVictim(CmdSender sender, String targetArg, ParseVictim how) {
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
	public CentralisedFuture<@Nullable UUIDAndAddress> parsePlayer(CmdSender sender, String targetArg) {
		return parseUUIDIfPossible(sender, targetArg, (uuid) -> {
			return uuidManager.lookupLastAddress(uuid).thenApply((optAddress) -> {
				if (optAddress.isEmpty()) {
					sender.sendMessage(notFound().player().replaceText("%TARGET%", targetArg));
					return null;
				}
				return new UUIDAndAddress(uuid, optAddress.get());
			});
		}, () -> uuidManager.lookupPlayer(targetArg).thenApply((optUuidAndAddress) -> {
			if (optUuidAndAddress.isEmpty()) {
				sender.sendMessage(notFound().player().replaceText("%TARGET%", targetArg));
				return null;
			}
			return optUuidAndAddress.get();
		}));
	}

	@Override
	public <R> @Nullable R parseScope(CmdSender sender, CommandPackage command, ParseScope<R> how) {

		boolean requirePermissions = configs.getScopeConfig().requirePermissions();
		ServerScope explicitScope;
		String specificServer, category, rawScopeInput;

		if ((specificServer = command.findHiddenArgumentSpecifiedValue("server")) != null) {
			explicitScope = scopeManager.specificScope(specificServer);

		} else if ((category = command.findHiddenArgumentSpecifiedValue("category")) != null) {
			explicitScope = scopeManager.category(category);

		} else if ((rawScopeInput = command.findHiddenArgumentSpecifiedValue("scope")) != null) {
			Optional<ServerScope> parsed = scopeManager.parseFrom(rawScopeInput);
			if (parsed.isEmpty()) {
				sender.sendMessage(all().scopes().invalid().replaceText("%SCOPE_ARG%", rawScopeInput));
				return null;
			}
			explicitScope = parsed.get();
		} else {
			if (requirePermissions && !sender.hasPermission("libertybans.scope.default")) {
				sender.sendMessage(all().scopes().noPermissionForDefault());
				return null;
			}
			return how.defaultValue(scopeManager);
		}
		if (requirePermissions) {
			String permissionSuffix = scopeManager.deconstruct(explicitScope, (type, value) -> {
				return switch (type) {
					case GLOBAL -> "global";
					case SERVER -> "server." + value;
					case CATEGORY -> "category." + value;
				};
			});
			if (!sender.hasPermission("libertybans.scope." + permissionSuffix)) {
				sender.sendMessage(all().scopes().noPermission().replaceText("%SCOPE%", formatter.formatScope(explicitScope)));
				return null;
			}
		}
		return how.explicitScope(explicitScope);
	}

}
