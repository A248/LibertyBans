/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.uuid;

import java.util.UUID;
import java.util.regex.Pattern;

public class StandardNameValidator implements NameValidator {

	private static final Pattern VANILLA_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]*+");

	public static NameValidator vanilla() {
		return new StandardNameValidator();
	}

	public static NameValidator createFromPrefix(String prefix) {
		if (prefix.isEmpty()) {
			throw new IllegalArgumentException("Name prefix must not be empty");
		}
		return new GeyserNameValidator(prefix);
	}

	@Override
	public String associatedPrefix() {
		return "";
	}

	@Override
	public boolean isVanillaName(String name) {
		return name.length() <= 16 && VANILLA_NAME_PATTERN.matcher(name).matches();
	}

	@Override
	public boolean isVanillaUUID(UUID uuid) {
		return true;
	}

	private static final class GeyserNameValidator extends StandardNameValidator {

		private final String namePrefix;

		private GeyserNameValidator(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		@Override
		public String associatedPrefix() {
			return namePrefix;
		}

		@Override
		public boolean isVanillaUUID(UUID uuid) {
			return uuid.getMostSignificantBits() != 0;
		}

	}
}
