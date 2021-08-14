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

import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.punish.MiscUtil;

@Singleton
public class AddressUnpunishCommands extends UnpunishCommands {

	@Inject
	public AddressUnpunishCommands(Dependencies dependencies,
								   PunishmentRevoker revoker, InternalFormatter formatter,
								   EnvEnforcer<?> envEnforcer, TabCompletion tabCompletion) {
		super(dependencies, Arrays.stream(MiscUtil.punishmentTypesExcludingKick()).map((type) -> "un" + type + "ip"),
				revoker, formatter, envEnforcer, tabCompletion);
	}

	@Override
	public PunishmentType parseType(String arg) {
		return PunishmentType.valueOf(arg.substring(2, arg.length() - 2));
	}

	@Override
	public CentralisedFuture<Victim> parseVictim(CmdSender sender, String targetArg) {
		return argumentParser().parseAddressVictim(sender, targetArg);
	}

	@Override
	public String getAdditionalPermission(PunishmentType type) {
		return "libertybans." + type + ".undoip";
	}

}
