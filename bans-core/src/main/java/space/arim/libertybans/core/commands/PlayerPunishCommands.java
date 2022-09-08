/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.commands.extra.ParsePlayerVictimDynamicallyComposite;
import space.arim.libertybans.core.commands.extra.PunishmentPermissionCheck;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.UUID;

@Singleton
public final class PlayerPunishCommands extends PunishCommands {

	private final EnvUserResolver envUserResolver;

	@Inject
	public PlayerPunishCommands(Dependencies dependencies, PunishmentDrafter drafter,
								InternalFormatter formatter, TabCompletion tabCompletion,
								EnvUserResolver envUserResolver) {
		super(dependencies, MiscUtil.punishmentTypes().stream().map(PunishmentType::toString),
				drafter, formatter, tabCompletion);
		this.envUserResolver = envUserResolver;
	}

	@Override
	public PunishmentType parseType(String arg)  {
		return PunishmentType.valueOf(arg);
	}

	@Override
	public CentralisedFuture<Victim> parseVictim(CmdSender sender, CommandPackage command,
												 String targetArg, PunishmentType type) {
		var victimFuture = argumentParser().parseVictim(
				sender, targetArg, new ParsePlayerVictimDynamicallyComposite(configs(), command)
		);
		switch (type) {
		case BAN:
		case MUTE:
		case WARN:
			return victimFuture;
		case KICK:
			// For kicks, users need to be screened to check that they are online
			if (configs().getSqlConfig().synchronization().enabled()) {
				// On a network with synchronization enabled, we do not check that the target is online
				return victimFuture;
			}
			return victimFuture.thenCompose((victim) -> {
				if (victim instanceof PlayerVictim) {
					UUID uuid = ((PlayerVictim) victim).getUUID();
					return envUserResolver.lookupName(uuid).thenApply((optName) -> {
						if (optName.isEmpty()) {
							sender.sendMessage(messages().additions().kicks().mustBeOnline().replaceText("%TARGET%", targetArg));
							return null;
						}
						return victim;
					});
				}
				return completedFuture(victim);
			});
		default:
			throw MiscUtil.unknownType(type);
		}
	}

	@Override
	public boolean hasTabCompletePermission(PunishmentPermissionCheck permissionCheck) {
		return permissionCheck.hasPermission(Victim.VictimType.PLAYER)
				|| permissionCheck.hasPermission(Victim.VictimType.COMPOSITE);
	}

}
