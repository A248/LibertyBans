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

package space.arim.libertybans.core.addon.checkuser;

import space.arim.injector.MultiBinding;
import space.arim.libertybans.core.addon.Addon;
import space.arim.libertybans.core.addon.AddonBindModule;
import space.arim.libertybans.core.commands.SubCommandGroup;

public final class CheckUserModule extends AddonBindModule {

	@MultiBinding
	public Addon<?> checkUserAddon(CheckUserAddon checkUserAddon) {
		return checkUserAddon;
	}

	@MultiBinding
	public SubCommandGroup checkUserCommand(CheckUserCommand checkUserCommand) {
		return checkUserCommand;
	}
}
