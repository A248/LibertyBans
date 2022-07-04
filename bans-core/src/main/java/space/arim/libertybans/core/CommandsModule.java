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

package space.arim.libertybans.core;

import space.arim.injector.MultiBinding;
import space.arim.libertybans.core.commands.AccountHistoryCommands;
import space.arim.libertybans.core.commands.AddonCommands;
import space.arim.libertybans.core.commands.AddressPunishCommands;
import space.arim.libertybans.core.commands.AddressUnpunishCommands;
import space.arim.libertybans.core.commands.AdminCommands;
import space.arim.libertybans.core.commands.AltCommands;
import space.arim.libertybans.core.commands.ImportCommands;
import space.arim.libertybans.core.commands.ListCommands;
import space.arim.libertybans.core.commands.PlayerPunishCommands;
import space.arim.libertybans.core.commands.PlayerUnpunishCommands;
import space.arim.libertybans.core.commands.SubCommandGroup;

public final class CommandsModule {

	@MultiBinding
	public SubCommandGroup playerPunishCommands(PlayerPunishCommands playerPunishCommands) {
		return playerPunishCommands;
	}

	@MultiBinding
	public SubCommandGroup addressPunishCommands(AddressPunishCommands addressPunishCommands) {
		return addressPunishCommands;
	}

	@MultiBinding
	public SubCommandGroup playerUnpunishCommands(PlayerUnpunishCommands playerUnpunishCommands) {
		return playerUnpunishCommands;
	}

	@MultiBinding
	public SubCommandGroup addressUnpunishCommands(AddressUnpunishCommands addressUnpunishCommands) {
		return addressUnpunishCommands;
	}

	@MultiBinding
	public SubCommandGroup listCommands(ListCommands listCommands) {
		return listCommands;
	}

	@MultiBinding
	public SubCommandGroup adminCommands(AdminCommands adminCommands) {
		return adminCommands;
	}

	@MultiBinding
	public SubCommandGroup importCommands(ImportCommands importCommands) {
		return importCommands;
	}

	@MultiBinding
	public SubCommandGroup altCommands(AltCommands altCommands) {
		return altCommands;
	}

	@MultiBinding
	public SubCommandGroup accountHistoryCommands(AccountHistoryCommands accountHistoryCommands) {
		return accountHistoryCommands;
	}

	@MultiBinding
	public SubCommandGroup addonCommands(AddonCommands addonCommands) {
		return addonCommands;
	}

}
