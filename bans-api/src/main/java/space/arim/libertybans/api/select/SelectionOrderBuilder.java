/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.select;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;

/**
 * Builder of {@link SelectionOrder}s. No required details
 * 
 * @author A248
 *
 */
public interface SelectionOrderBuilder {

	/**
	 * Sets the punishment type matched. Use {@code null} to match all types, which is
	 * the default behavior
	 * 
	 * @param type the punishment type or {@code null} for all types
	 * @return this builder
	 */
	SelectionOrderBuilder type(PunishmentType type);

	/**
	 * Sets the victim matched. Use {@code null} to match all victims, which is the
	 * default behavior
	 * 
	 * @param victim the victim or {@code null} for all victims
	 * @return this builder
	 */
	SelectionOrderBuilder victim(Victim victim);

	/**
	 * Sets the operator matched. Use {@code null} to match all operators, which is
	 * the default behavior
	 * 
	 * @param operator the operator or {@code null} for all operators
	 * @return this builder
	 */
	SelectionOrderBuilder operator(Operator operator);

	/**
	 * Sets the scope matched. Use {@code null} to match all scopes, which is the
	 * default behavior
	 * 
	 * @param scope the scope or {@code null} for all scopes
	 * @return this builder
	 */
	SelectionOrderBuilder scope(ServerScope scope);

	/**
	 * Sets whether only active punishments should be matched. True by default,
	 * meaning only active punishments are selected. <br>
	 * <br>
	 * Active punishments are those not expired and not undone.
	 * 
	 * @param selectActiveOnly whether to select active punishments only
	 * @return this builder
	 */
	SelectionOrderBuilder selectActiveOnly(boolean selectActiveOnly);

	/**
	 * Sets that only active punishments should be matched. Enabled by default,
	 * meaning only active punishments are selected. <br>
	 * <br>
	 * Active punishments are those not expired and not undone.
	 * 
	 * @return this builder
	 */
	default SelectionOrderBuilder selectActiveOnly() {
		return selectActiveOnly(true);
	}

	/**
	 * Sets that all punishments, not just those active, should be matched. When
	 * this method is used, historical punishments and expired punishments are
	 * selected. <br>
	 * <br>
	 * Active punishments are those not expired and not undone.
	 * 
	 * @return this builder
	 */
	default SelectionOrderBuilder selectAll() {
		return selectActiveOnly(false);
	}

	/**
	 * Sets the amount of punishments to skip when retrieving. No punishments are
	 * skipped by default. <br>
	 * <br>
	 * For example, this may be used to implement pagination on a user interface.
	 * Punishments on earlier pages need not be displayed.
	 * 
	 * @param skipCount the amount of punishments to skip
	 * @return this builder
	 * @throws IllegalArgumentException if {@code skipCount} is negative
	 */
	SelectionOrderBuilder skipFirstRetrieved(int skipCount);

	/**
	 * Sets the maximum amount of punishments to retrieve. Unlimited by default.
	 * <br>
	 * <br>
	 * Note: If this is used with {@link #skipFirstRetrieved(int)}, the amount of
	 * punishments retrieved is equal to {@code maximumToRetrieve - skipCount}
	 * 
	 * @param maximumToRetrieve the max amount of punishments to retrieve, {@code 0}
	 *                          for unlimited
	 * @return this builder
	 * @throws IllegalArgumentException if {@code maximumToRetrieve} is negative
	 */
	SelectionOrderBuilder maximumToRetrieve(int maximumToRetrieve);

	/**
	 * Builds a {@link SelectionOrder} from the details of this builder. May be used
	 * repeatedly without side effects.
	 * 
	 * @return a selection order from this builder's details
	 * @throws IllegalStateException if the required details on this builder have not been set
	 */
	SelectionOrder build();

}
