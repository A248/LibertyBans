/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.it;

import java.util.Objects;

final class ConfigSpecWithDatabase implements InstanceKey {

	private final ConfigSpec configSpec;
	private final DatabaseInstance databaseInstance;

	ConfigSpecWithDatabase(ConfigSpec configSpec, DatabaseInstance databaseInstance) {
		this.configSpec = Objects.requireNonNull(configSpec, "configSpec");
		this.databaseInstance = Objects.requireNonNull(databaseInstance, "databaseInstance");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ConfigSpecWithDatabase that = (ConfigSpecWithDatabase) o;
		return databaseInstance.equals(that.databaseInstance) && configSpec.equals(that.configSpec);
	}

	@Override
	public int hashCode() {
		int result = databaseInstance.hashCode();
		result = 31 * result + configSpec.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ConfigSpecWithDatabase{" +
				"databaseInstance=" + databaseInstance +
				"configSpec=" + configSpec +
				'}';
	}
}
