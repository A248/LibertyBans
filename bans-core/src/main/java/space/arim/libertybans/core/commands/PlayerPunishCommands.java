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
package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.punish.MiscUtil;

import java.util.UUID;

@Singleton
public class PlayerPunishCommands extends PunishCommands {

	private final EnvUserResolver envUserResolver;

	@Inject
	public PlayerPunishCommands(Dependencies dependencies,
								PunishmentDrafter drafter, InternalFormatter formatter,
								EnvEnforcer<?> envEnforcer, TabCompletion tabCompletion,
								EnvUserResolver envUserResolver) {
		super(dependencies, MiscUtil.punishmentTypes().stream().map(PunishmentType::toString),
				drafter, formatter, envEnforcer, tabCompletion);
		this.envUserResolver = envUserResolver;
	}

	@Override
	public PunishmentType parseType(String arg)  {
		return PunishmentType.valueOf(arg);
	}

	@Override
	public CentralisedFuture<Victim> parseVictim(CmdSender sender, String targetArg, PunishmentType type) {
		var victimFuture = argumentParser().parseVictimByName(sender, targetArg);
		switch (type) {
		case BAN:
		case MUTE:
		case WARN:
			return victimFuture;
		case KICK:
			// For kicks, users need to be screened to check that they are online
			return victimFuture.thenApply((victim) -> {
				if (victim instanceof PlayerVictim) {
					UUID uuid = ((PlayerVictim) victim).getUUID();
					if (envUserResolver.lookupName(uuid).isEmpty()) {
						sender.sendMessage(messages().additions().kicks().mustBeOnline().replaceText("%TARGET%", targetArg));
						return null;
					}
				}
				return victim;
			});
		default:
			throw MiscUtil.unknownType(type);
		}
	}

}
