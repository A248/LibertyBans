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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.AdditionAssistant;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.punish.permission.VictimTypeCheck;

@Singleton
public final class AddressPunishCommands extends PunishCommands implements PunishUnpunishCommands.WithPreferredVictim {

	@Inject
	public AddressPunishCommands(Dependencies dependencies, PunishmentDrafter drafter, InternalFormatter formatter,
								 AdditionAssistant additionAssistant, TabCompletion tabCompletion) {
		super(dependencies, MiscUtil.punishmentTypes().stream().map((type) -> "ip" + type),
				drafter, formatter, additionAssistant, tabCompletion);
	}

	@Override
	public PunishmentType parseType(String arg) {
		return PunishmentType.valueOf(arg.substring(2));
	}

	@Override
	public Victim.VictimType preferredVictimType() {
		return Victim.VictimType.ADDRESS;
	}

	@Override
	public boolean hasTabCompletePermission(VictimTypeCheck permissionCheck) {
		return permissionCheck.hasPermission(Victim.VictimType.ADDRESS);
	}

}
