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

package space.arim.libertybans.bootstrap;

import java.util.Objects;

public final class ClassPresence {

	private final String className;
	private final String dependencyName;

	public ClassPresence(String className, String dependencyName) {
		this.className = Objects.requireNonNull(className, "className");
		this.dependencyName = Objects.requireNonNull(dependencyName, "dependencyName");
	}

	public String className() {
		return className;
	}

	public String dependencyName() {
		return dependencyName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClassPresence that = (ClassPresence) o;
		return className.equals(that.className) && dependencyName.equals(that.dependencyName);
	}

	@Override
	public int hashCode() {
		int result = className.hashCode();
		result = 31 * result + dependencyName.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ClassPresence{" +
				"className='" + className + '\'' +
				", dependencyName='" + dependencyName + '\'' +
				'}';
	}
}
