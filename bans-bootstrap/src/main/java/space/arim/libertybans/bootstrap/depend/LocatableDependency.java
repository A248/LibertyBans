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

package space.arim.libertybans.bootstrap.depend;

import java.util.Objects;

/**
 * Dependency with associated repository
 *
 */
public final class LocatableDependency {

	private final Dependency dependency;
	private final Repository repository;

	public LocatableDependency(Dependency dependency, Repository repository) {
		this.dependency = Objects.requireNonNull(dependency, "dependency");
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	public Dependency dependency() {
		return dependency;
	}

	public Repository repository() {
		return repository;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LocatableDependency that = (LocatableDependency) o;
		return dependency.equals(that.dependency) && repository.equals(that.repository);
	}

	@Override
	public int hashCode() {
		int result = dependency.hashCode();
		result = 31 * result + repository.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "LocatableDependency{" +
				"dependency=" + dependency +
				", repository=" + repository +
				'}';
	}
}
