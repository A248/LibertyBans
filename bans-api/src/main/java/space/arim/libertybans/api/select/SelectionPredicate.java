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

import java.util.Arrays;
import java.util.Set;

/**
 * How a certain criterion can be used to exclude punishments from a {@link SelectionOrder}. <br>
 * <br>
 * For most use cases, the static factory methods can be used to create instances.
 * For advanced uses, it is also possible to extend this class directly; implementors
 * must ensure that returned {@code Set}s are immutable and thread safe.
 *
 * @param <U> the type of the criterion
 */
public abstract class SelectionPredicate<U> {

	/**
	 * Values accepted in the selection. Values other than the ones specified here
	 * will be implicitly rejected and thus will not appear in the selection. <br>
	 * <br>
	 * To accept all values, use an empty set. The empty set accepts all.
	 *
	 * @return the values accepted, or an empty set to accept all values
	 */
	public abstract Set<U> acceptedValues();

	/**
	 * Values rejected in the selection. Values specified here will not appear in the selection. <br>
	 * <br>
	 * An empty set rejects no values.
	 *
	 * @return the values rejected
	 */
	public abstract Set<U> rejectedValues();

	/**
	 * Convenience method to determine whether {@link #acceptedValues()} is a single value
	 * and {@link #rejectedValues()} is empty
	 *
	 * @return true if and only if this predicate checks only a single accepted value
	 */
	public final boolean isSimpleEquality() {
		return rejectedValues().isEmpty() && acceptedValues().size() == 1;
	}

	/**
	 * Convenience method which is the opposite of {@link #isSimpleEquality()}
	 *
	 * @return false if and only if this predicate checks only a single accepted value
	 */
	public final boolean isNotSimpleEquality() {
		return !isSimpleEquality();
	}

	/**
	 * A selection predicate is equal to another if and only if {@link #acceptedValues()}
	 * and {@link #rejectedValues()} are equal
	 *
	 * @param o the other object
	 * @return true if equal, false otherwise
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SelectionPredicate)) return false;
		SelectionPredicate<?> that = (SelectionPredicate<?>) o;
		return acceptedValues().equals(that.acceptedValues())
				&& rejectedValues().equals(that.rejectedValues());
	}

	@Override
	public final int hashCode() {
		int result = acceptedValues().hashCode();
		result = 31 * result + rejectedValues().hashCode();
		return result;
	}

	/**
	 * Creates a seletion predicate which matches anything and everything
	 *
	 * @param <U> the type of the criterion
	 * @return the predicate
	 */
	public static <U> SelectionPredicate<U> matchingAll() {
		return AllPredicate.instance();
	}

	/**
	 * Creates a selection predicate which matches only a single accepted value
	 *
	 * @param value the value to match
	 * @param <U> the type of the criterion
	 * @return the predicate
	 */
	public static <U> SelectionPredicate<U> matchingOnly(U value) {
		return new SimpleEqualityPredicate<>(value);
	}

	/**
	 * Creates a selection predicate which matches the given accepted values
	 *
	 * @param first the first value to match
	 * @param extra the other values to match
	 * @param <U> the type of the criterion
	 * @return the predicate
	 */
	@SafeVarargs
	public static <U> SelectionPredicate<U> matchingAnyOf(U first, U...extra) {
		if (extra.length == 0) {
			return matchingOnly(first);
		}
		U[] combined = Arrays.copyOf(extra, extra.length + 1);
		combined[extra.length] = first;
		return new AnyOfPredicate<>(Set.of(combined));
	}

	/**
	 * Creates a selection predicate which matches the given accepted values
	 *
	 * @param values the values to match. Must not be empty
	 * @param <U> the type of the criterion
	 * @return the predicate
	 * @throws IllegalArgumentException if {@code values} is empty
	 */
	public static <U> SelectionPredicate<U> matchingAnyOf(Set<U> values) {
		values = Set.copyOf(values);
		return switch (values.size()) {
			case 0 -> throw new IllegalArgumentException("Values must be not be empty");
			case 1 -> matchingOnly(values.iterator().next());
			default -> new AnyOfPredicate<>(values);
		};
	}

	/**
	 * Creates a selection predicate which rejects the given values and matches all other values
	 *
	 * @param values the values to reject
	 * @param <U> the type of the criterion
	 * @return the predicate
	 */
	@SafeVarargs
	public static <U> SelectionPredicate<U> matchingNone(U...values) {
		return matchingNone(Set.of(values));
	}

	/**
	 * Creates a selection predicate which rejects the given values and matches all other values
	 *
	 * @param values the values to reject
	 * @param <U> the type of the criterion
	 * @return the predicate
	 */
	public static <U> SelectionPredicate<U> matchingNone(Set<U> values) {
		return new NoneOfPredicate<>(values);
	}
}
