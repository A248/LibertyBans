/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import java.util.Objects;

public final class Platform {

	private final Category category;
	private final String platformName;
	private final boolean slf4j;
	private final boolean kyoriAdventure;
	
	Platform(Category category, String platformName,
			 boolean slf4j, boolean kyoriAdventure) {
		this.category = Objects.requireNonNull(category, "category");
		this.platformName = Objects.requireNonNull(platformName, "platformName");
		this.slf4j = slf4j;
		this.kyoriAdventure = kyoriAdventure;
	}

	public Category category() {
		return category;
	}

	public String platformName() {
		return platformName;
	}

	public boolean hasSlf4jSupport() {
		return slf4j;
	}

	public boolean hasKyoriAdventureSupport() {
		return kyoriAdventure;
	}

	public static Builder forCategory(Category category) {
		return new Builder(category);
	}

	public static final class Builder {

		private boolean slf4j;
		private boolean kyoriAdventure;
		private final Category category;

		private Builder(Category category) {
			this.category = Objects.requireNonNull(category, "category");
		}

		public Builder slf4jSupport(boolean slf4j) {
			this.slf4j = slf4j;
			return this;
		}

		public Builder kyoriAdventureSupport(boolean kyoriAdventure) {
			this.kyoriAdventure = kyoriAdventure;
			return this;
		}

		public Platform build(String platformName) {
			return new Platform(category, platformName, slf4j, kyoriAdventure);
		}
	}
	
	public enum Category {
		BUKKIT,
		BUNGEE,
		VELOCITY
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Platform platform = (Platform) o;
		return slf4j == platform.slf4j && kyoriAdventure == platform.kyoriAdventure && category == platform.category && platformName.equals(platform.platformName);
	}

	@Override
	public int hashCode() {
		int result = category.hashCode();
		result = 31 * result + platformName.hashCode();
		result = 31 * result + (slf4j ? 1 : 0);
		result = 31 * result + (kyoriAdventure ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Platform{" +
				"category=" + category +
				", platformName='" + platformName + '\'' +
				", slf4j=" + slf4j +
				", kyoriAdventure=" + kyoriAdventure +
				'}';
	}
}
