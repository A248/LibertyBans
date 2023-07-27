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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.env.CmdSender;

import java.util.stream.Stream;

/**
 * Tab completion. Methods should be called from and only from the same thread as that on which
 * tab completion was requested by the platform.
 *
 */
public interface TabCompletion extends Part {

	Stream<String> completeOnlinePlayerNames(CmdSender sender);

	Stream<String> completeOfflinePlayerNames(CmdSender sender);

	Stream<String> completePunishmentDurations(CmdSender sender, PunishmentType type);

}
