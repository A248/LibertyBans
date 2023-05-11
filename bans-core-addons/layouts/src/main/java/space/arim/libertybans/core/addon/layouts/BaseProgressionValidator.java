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

import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.validator.ValueValidator;
import space.arim.libertybans.core.addon.layouts.Track.Ladder.Progression;

import java.util.Map;

public final class BaseProgressionValidator implements ValueValidator {
	@Override
	public void validate(String key, Object value) throws BadValueException {
		@SuppressWarnings("unchecked")
		Map<Integer, Progression> progressions = (Map<Integer, Progression>) value;
		if (!progressions.containsKey(1)) {
			throw new BadValueException.Builder()
					.key(key)
					.message("There must be a progression defined for the first punishment")
					.build();
		}
	}
}
