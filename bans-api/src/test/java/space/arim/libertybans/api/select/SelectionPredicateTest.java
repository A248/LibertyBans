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

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectionPredicateTest {

	@Test
	public void matchAll() {
		var selection = SelectionPredicate.matchingAll();
		assertEquals(Set.of(), selection.acceptedValues());
		assertEquals(Set.of(), selection.rejectedValues());
		assertFalse(selection.isSimpleEquality());
	}

	@Test
	public void matchOnly() {
		var selection = SelectionPredicate.matchingOnly(3);
		assertEquals(Set.of(3), selection.acceptedValues());
		assertEquals(Set.of(), selection.rejectedValues());
		assertTrue(selection.isSimpleEquality());
	}

	@Test
	public void matchAnyOfSingle() {
		var selection = SelectionPredicate.matchingAnyOf(3);
		assertEquals(Set.of(3), selection.acceptedValues());
		assertEquals(Set.of(), selection.rejectedValues());
		assertTrue(selection.isSimpleEquality());
	}

	@Test
	public void matchAnyOfMultiple() {
		var selection = SelectionPredicate.matchingAnyOf(3, 2, 1, 5);
		assertEquals(Set.of(3, 2, 1, 5), selection.acceptedValues());
		assertEquals(Set.of(), selection.rejectedValues());
		assertFalse(selection.isSimpleEquality());
	}

	@Test
	public void matchNone() {
		var selection = SelectionPredicate.matchingNone(-4, 2, 0);
		assertEquals(Set.of(), selection.acceptedValues());
		assertEquals(Set.of(-4, 2, 0), selection.rejectedValues());
		assertFalse(selection.isSimpleEquality());
	}
}
