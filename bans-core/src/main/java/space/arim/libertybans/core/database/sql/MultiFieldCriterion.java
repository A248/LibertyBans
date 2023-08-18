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

package space.arim.libertybans.core.database.sql;

import org.jooq.Condition;
import space.arim.libertybans.api.select.SelectionPredicate;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.not;

public interface MultiFieldCriterion<F> {

	Condition matchesValue(F value);

	default Condition buildCondition(SelectionPredicate<F> selection) {
		// Check field is accepted
		Condition fieldAcceptedCondition = noCondition();
		for (F acceptedValue : selection.acceptedValues()) {
			fieldAcceptedCondition = fieldAcceptedCondition.or(matchesValue(acceptedValue));
		}
		// Check field is not rejected
		Condition fieldNotRejectedCondition = noCondition();
		for (F rejectedValue : selection.rejectedValues()) {
			fieldNotRejectedCondition = fieldNotRejectedCondition.and(not(matchesValue(rejectedValue)));
		}
		return fieldAcceptedCondition.and(fieldNotRejectedCondition);
	}

}
