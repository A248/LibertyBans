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

package space.arim.libertybans.core.selector;

import org.jooq.Condition;
import org.jooq.Field;
import space.arim.libertybans.api.select.SelectionPredicate;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.val;

record SingleFieldCriterion<F>(Field<F> field) {

	private Field<F> inlineIfNeeded(F value) {
		// Automatically inline enum comparisons (e.g. PunishmentType, VictimType, and ScopeType)
		Class<F> fieldType = field.getType();
		boolean shouldInline = fieldType.isEnum();
		return shouldInline ? inline(value) : val(value);
	}

	private static boolean containsNull(Set<?> set) {
		// Some sets throw NPE on contains(null)
		for (Object o : set) {
			if (o == null) {
				return true;
			}
		}
		return false;
	}

	private Condition matches(Set<F> acceptedValues, Set<F> rejectedValues) {
		Condition acceptedCondition = switch (acceptedValues.size()) {
			case 0 -> noCondition();
			case 1 -> {
				var singleAcceptedValue = acceptedValues.iterator().next();
				if (singleAcceptedValue == null) {
					yield field.isNull();
				}
				yield field.eq(inlineIfNeeded(singleAcceptedValue));
			}
			default -> {
				if (containsNull(acceptedValues)) {
					yield field.in(acceptedValues).or(field.isNull());
				} else {
					yield field.in(acceptedValues);
				}
			}
		};
		Condition notRejectedCondition = switch (rejectedValues.size()) {
			case 0 -> noCondition();
			case 1 -> {
				var singleRejectedValue = rejectedValues.iterator().next();
				if (singleRejectedValue == null) {
					yield field.isNotNull();
				}
				yield field.notEqual(inlineIfNeeded(singleRejectedValue));
			}
			default -> {
				if (containsNull(rejectedValues)) {
					yield field.notIn(rejectedValues).and(field.isNotNull());
				} else {
					yield field.notIn(rejectedValues);
				}
			}
		};
		return acceptedCondition.and(notRejectedCondition);
	}

	Condition matches(SelectionPredicate<F> selection) {
		return matches(selection.acceptedValues(), selection.rejectedValues());
	}

	<G> Condition matches(SelectionPredicate<G> selection, Function<G, F> converter) {
		class SetView extends AbstractSet<F> {

			private final Set<G> original;

			SetView(Set<G> original) {
				this.original = original;
			}

			@Override
			public Iterator<F> iterator() {
				Iterator<G> iter = original.iterator();
				class IteratorView implements Iterator<F> {

					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public F next() {
						return converter.apply(iter.next());
					}

					@Override
					public void forEachRemaining(Consumer<? super F> action) {
						iter.forEachRemaining((g) -> action.accept(converter.apply(g)));
					}
				}
				return new IteratorView();
			}

			@Override
			public int size() {
				return original.size();
			}
		}
		return matches(
				new SetView(selection.acceptedValues()), new SetView(selection.rejectedValues())
		);
	}

}
