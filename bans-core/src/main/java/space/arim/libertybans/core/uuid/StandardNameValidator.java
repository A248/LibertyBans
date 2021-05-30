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
package space.arim.libertybans.core.uuid;

import java.util.Objects;
import java.util.regex.Pattern;

public final class StandardNameValidator implements NameValidator {

	private final Pattern validNamePattern;

	public StandardNameValidator(Pattern validNamePattern) {
		this.validNamePattern = Objects.requireNonNull(validNamePattern);
	}

	@Override
	public boolean validateNameArgument(String name) {
		// Geyser/Floodgate ensures player names are less than 16 characters
		return name.length() <= 16 && validNamePattern.matcher(name).matches();
	}
	
}
