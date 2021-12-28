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

package space.arim.libertybans.api.select;

import java.util.Set;

final class AnyOfPredicate<U> extends SelectionPredicate<U> {

	private final Set<U> acceptedValues;

	AnyOfPredicate(Set<U> acceptedValues) {
		acceptedValues = Set.copyOf(acceptedValues);
		assert acceptedValues.size() != 1;
		this.acceptedValues = acceptedValues;
	}

	@Override
	public Set<U> acceptedValues() {
		return acceptedValues;
	}

	@Override
	public Set<U> rejectedValues() {
		return Set.of();
	}

	@Override
	public String toString() {
		return "AnyOfPredicate{" +
				"acceptedValues=" + acceptedValues +
				'}';
	}
}
