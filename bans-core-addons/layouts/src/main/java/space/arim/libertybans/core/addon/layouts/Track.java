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

package space.arim.libertybans.core.addon.layouts;

import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfValidator;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.scope.ConfiguredScope;

import java.util.Map;

public record Track(String id, Ladder ladder) {

	public interface Ladder {

		@ConfKey("count-active")
		boolean countActive();

		@ConfValidator(BaseProgressionValidator.class)
		Map<Integer, @SubSection Progression> progressions();

		interface Progression {

			PunishmentType type();

			String reason();

			ParsedDuration duration();

			ConfiguredScope scope();

		}
	}

}
