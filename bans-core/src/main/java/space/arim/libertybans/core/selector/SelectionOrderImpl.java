/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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
import org.jooq.Record;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.core.database.sql.DeserializedVictim;
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.VictimCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.not;

final class SelectionOrderImpl extends SelectionBaseSQL implements SelectionOrder {

	private final SelectionPredicate<Victim> victims;
	private final SelectionPredicate<Victim.VictimType> victimTypes;

	SelectionOrderImpl(Details details, Resources resources,
					   SelectionPredicate<Victim> victims, SelectionPredicate<Victim.VictimType> victimTypes) {
		super(details, resources);
		this.victims = Objects.requireNonNull(victims, "victims");
		this.victimTypes = Objects.requireNonNull(victimTypes, "victimTypes");
	}

	@Override
	public SelectionPredicate<Victim> getVictims() {
		return victims;
	}

	@Override
	public SelectionPredicate<Victim.VictimType> getVictimTypes() {
		return victimTypes;
	}

	@Override
	Query<?> requestQuery(QueryParameters parameters) {
		PunishmentFields fields = requestSimpleView();

		List<Field<?>> additionalColumns = new ArrayList<>(3);
		if (getVictims().isNotSimpleEquality()) {
			if (getVictimTypes().isNotSimpleEquality()) {
				additionalColumns.add(fields.victimType());
			}
			additionalColumns.add(fields.victimUuid());
			additionalColumns.add(fields.victimAddress());
		}
		Condition additionalPredication;
		{
			VictimCondition victimCondition = new VictimCondition(fields);
			// Check victim is accepted
			Condition victimAcceptedCondition = noCondition();
			for (Victim acceptedVictim : getVictims().acceptedValues()) {
				victimAcceptedCondition = victimAcceptedCondition.or(
						victimCondition.matchesVictim(acceptedVictim)
				);
			}
			// Check victim is not rejected
			Condition victimNotRejectedCondition = noCondition();
			for (Victim rejectedVictim : getVictims().rejectedValues()) {
				Condition notEqual = not(
						victimCondition.matchesVictim(rejectedVictim)
				);
				victimNotRejectedCondition = victimNotRejectedCondition.and(notEqual);
			}
			// Check victim type is accepted
			additionalPredication = matchesCriterion(getVictimTypes(), fields.victimType())
					.and(victimAcceptedCondition)
					.and(victimNotRejectedCondition);
		}
		return new QueryBuilder(fields, additionalColumns, additionalPredication) {
			@Override
			Victim victimFromRecord(Record record) {
				if (getVictims().isSimpleEquality()) {
					return getVictims().acceptedValues().iterator().next();
				} else {
					Victim.VictimType victimType = retrieveValueFromRecordOrSelection(
							getVictimTypes(), record, fields.victimType()
					);
					return new DeserializedVictim(
							record.get(fields.victimUuid()),
							record.get(fields.victimAddress())
					).victim(victimType);
				}
			}
		}.constructSelect(parameters, fields.table());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SelectionOrderImpl that = (SelectionOrderImpl) o;
		return victims.equals(that.victims) && victimTypes.equals(that.victimTypes);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + victims.hashCode();
		result = 31 * result + victimTypes.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SelectionOrderImpl{" +
				"victims=" + victims +
				", victimTypes=" + victimTypes +
				", types=" + getTypes() +
				", operators=" + getOperators() +
				", scopes=" + getScopes() +
				", selectActiveOnly=" + selectActiveOnly() +
				", skipCount=" + skipCount() +
				", limitToRetrieve=" + limitToRetrieve() +
				", seekAfterStartTime=" + seekAfterStartTime() +
				", seekAfterId=" + seekAfterId() +
				", seekBeforeStartTime=" + seekBeforeStartTime() +
				", seekBeforeId=" + seekBeforeId() +
				'}';
	}

}
