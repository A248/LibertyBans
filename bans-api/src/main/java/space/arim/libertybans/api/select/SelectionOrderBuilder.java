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

package space.arim.libertybans.api.select;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;

import java.time.Instant;

/**
 * Builder for {@link SelectionOrder}. No required details
 *
 */
public interface SelectionOrderBuilder extends SelectionBuilderBase<SelectionOrderBuilder, SelectionOrder> {

	/**
	 * Sets the victim matched. All are matched by default. <br>
	 * <br>
	 * Please see {@link #victims(SelectionPredicate)} for how this interacts with composite victims.
	 * 
	 * @param victim the victim
	 * @return this builder
	 */
	default SelectionOrderBuilder victim(Victim victim) {
		return victims(SelectionPredicate.matchingOnly(victim));
	}

	/**
	 * Sets the victims matched. All are matched by default. <br>
	 * <br>
	 * Composite victims have somewhat special handling which reflects their purpose as an "either-or"
	 * kind of victim. That is, specifying a composite victim in set of the accepted victims will
	 * select all punishments which <i>partially</i> but not entirely match that composite victim.
	 * Formally, if the selected victim is a composite victim, and its UUID matches the specified
	 * composite victim  OR its address matches the specified composite, then the selected victim is matched. <br>
	 * <br>
	 * For example, specifying a composite victim of <code>new UUID(1, 1)</code> and address 127.0.0.1
	 * will match all composite victims whose UUID is <code>new UUID(1, 1)</code> OR whose address is
	 * 127.0.0.1. However, specifying a composite victim will <i>not</i> match other kinds of victims.
	 *
	 * @param victims the victim predicate
	 * @return this builder
	 */
	SelectionOrderBuilder victims(SelectionPredicate<Victim> victims);

	/**
	 * Sets the victim type matched. All are matched by default.
	 *
	 * @param victimType the victim type to match
	 * @return this builder
	 */
	default SelectionOrderBuilder victimType(Victim.VictimType victimType) {
		return victimTypes(SelectionPredicate.matchingOnly(victimType));
	}

	/**
	 * Sets the victim types matched. All are matched by default.
	 *
	 * @param victimTypes the victim types
	 * @return this builder
	 */
	SelectionOrderBuilder victimTypes(SelectionPredicate<Victim.VictimType> victimTypes);

	/*
	Redeclare methods from SelectionBuilderBase for 1.0 API compatibility
	 */

	@Override
	default SelectionOrderBuilder type(PunishmentType type) {
		return types(SelectionPredicate.matchingOnly(type));
	}

	@Override
	SelectionOrderBuilder types(SelectionPredicate<PunishmentType> types);

	@Override
	default SelectionOrderBuilder operator(Operator operator) {
		return SelectionBuilderBase.super.operator(operator);
	}

	@Override
	SelectionOrderBuilder operators(SelectionPredicate<Operator> operators);

	@Override
	default SelectionOrderBuilder scope(ServerScope scope) {
		return SelectionBuilderBase.super.scope(scope);
	}

	@Override
	SelectionOrderBuilder scopes(SelectionPredicate<ServerScope> scopes);

	@Override
	SelectionOrderBuilder selectActiveOnly(boolean selectActiveOnly);

	@Override
	default SelectionOrderBuilder selectActiveOnly() {
		return SelectionBuilderBase.super.selectActiveOnly();
	}

	@Override
	default SelectionOrderBuilder selectAll() {
		return SelectionBuilderBase.super.selectAll();
	}

	@Override
	SelectionOrderBuilder skipFirstRetrieved(int skipCount);

	@Override
	SelectionOrderBuilder limitToRetrieve(int limitToRetrieve);

	@Override
	SelectionOrderBuilder seekAfter(Instant minimumStartTime, long minimumId);

	@Override
	SelectionOrder build();

}
