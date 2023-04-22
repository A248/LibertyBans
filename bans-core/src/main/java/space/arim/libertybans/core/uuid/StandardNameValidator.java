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

package space.arim.libertybans.core.uuid;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class StandardNameValidator implements NameValidator {

	private final Pattern validNamePattern;

	private static final String VANILLA_NAME_PATTERN = "[a-zA-Z0-9_]*+";

	private StandardNameValidator(Pattern validNamePattern) {
		this.validNamePattern = Objects.requireNonNull(validNamePattern);
	}

	public static NameValidator vanilla() {
		return new StandardNameValidator(Pattern.compile(VANILLA_NAME_PATTERN));
	}

	public static NameValidator createFromPrefix(String prefix) {
		if (prefix.isEmpty()) {
			// Avoid quoting empty strings (bad regex practice)
			throw new IllegalArgumentException("Name prefix must not be empty");
		}
		String validNameRegex = "(" + Pattern.quote(prefix) + ")?" + VANILLA_NAME_PATTERN;
		return new GeyserNameValidator(prefix, Pattern.compile(validNameRegex));
	}

	@Override
	public String associatedPrefix() {
		return "";
	}

	@Override
	public boolean validateNameArgument(String name) {
		// Geyser/Floodgate ensures player names are less than 16 characters
		return name.length() <= 16 && validNamePattern.matcher(name).matches();
	}

	@Override
	public boolean isVanillaName(String name) {
		return true;
	}

	@Override
	public boolean isVanillaUUID(UUID uuid) {
		return true;
	}

	private static final class GeyserNameValidator extends StandardNameValidator {

		private final String namePrefix;

		private GeyserNameValidator(String namePrefix, Pattern validNamePattern) {
			super(validNamePattern);
			this.namePrefix = namePrefix;
		}

		@Override
		public String associatedPrefix() {
			return namePrefix;
		}

		@Override
		public boolean isVanillaName(String name) {
			return !name.startsWith(namePrefix);
		}

		@Override
		public boolean isVanillaUUID(UUID uuid) {
			return uuid.getMostSignificantBits() != 0;
		}

	}
}
