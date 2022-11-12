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

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;

import java.time.Instant;

/**
 * Builder of {@link SelectionOrder}s. No required details
 * 
 * @author A248
 *
 */
public interface SelectionOrderBuilder {

	/**
	 * Sets the punishment type matched
	 * 
	 * @param type the punishment type
	 * @return this builder
	 */
	default SelectionOrderBuilder type(PunishmentType type) {
		return types(SelectionPredicate.matchingOnly(type));
	}

	/**
	 * Sets the punishment types matched
	 *
	 * @param types the punishment type predicate
	 * @return this builder
	 */
	SelectionOrderBuilder types(SelectionPredicate<PunishmentType> types);

	/**
	 * Sets the victim matched. Please see {@link #victims(SelectionPredicate)} for how this
	 * interacts with composite victims.
	 * 
	 * @param victim the victim
	 * @return this builder
	 */
	default SelectionOrderBuilder victim(Victim victim) {
		return victims(SelectionPredicate.matchingOnly(victim));
	}

	/**
	 * Sets the victims matched. <br>
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
	 * Sets the operator matched
	 * 
	 * @param operator the operator
	 * @return this builder
	 */
	default SelectionOrderBuilder operator(Operator operator) {
		return operators(SelectionPredicate.matchingOnly(operator));
	}

	/**
	 * Sets the operators matched
	 *
	 * @param operators the operator predicate
	 * @return this builder
	 */
	SelectionOrderBuilder operators(SelectionPredicate<Operator> operators);

	/**
	 * Sets the scope matched
	 * 
	 * @param scope the scope
	 * @return this builder
	 */
	default SelectionOrderBuilder scope(ServerScope scope) {
		return scopes(SelectionPredicate.matchingOnly(scope));
	}

	/**
	 * Sets the scopes matched
	 *
	 * @param scopes the scope predicate
	 * @return this builder
	 */
	SelectionOrderBuilder scopes(SelectionPredicate<ServerScope> scopes);

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
	 * This method may be used to implement offset pagination of selected punishments,
	 * together with {@link #limitToRetrieve(int)}. <br>
	 * <br>
	 * Behavior is unspecified if this is used simultaneously with {@link #seekAfter(Instant, long)}.
	 * Callers should consider the methods exclusive to avoid unspecified behavior.
	 * 
	 * @param skipCount the amount of punishments to skip
	 * @return this builder
	 * @throws IllegalArgumentException if {@code skipCount} is negative
	 */
	SelectionOrderBuilder skipFirstRetrieved(int skipCount);

	/**
	 * Sets the amount of punishments to retrieve. Unlimited by default.
	 * <br>
	 * <br>
	 * If this is used with {@link #skipFirstRetrieved(int)}, the amount of punishments
	 * retrieved is still equal to {@code limitToRetrieve}. In other words, this
	 * method is the same as SQL's {@code LIMIT} clause.
	 * 
	 * @param limitToRetrieve the amount of punishments to retrieve, {@code 0} for unlimited
	 * @return this builder
	 * @throws IllegalArgumentException if {@code limitToRetrieve} is negative
	 */
	SelectionOrderBuilder limitToRetrieve(int limitToRetrieve);

	/**
	 * Sets the time after which punishments will be retrieved. Only punishments whose
	 * start time ({@link Punishment#getStartDate()}) is after or equal to the given value will be
	 * selected. <br>
	 * <br>
	 * If a punishment has a start time equal to {@code minimumStartTime}, its ID will be checked against
	 * {@code minimumId}. The punishment will then be selected if its ID is equal to or greater than
	 * {@code minimumId}. <br>
	 * <br>
	 * To reset this option to the default value, use a minimum start time of {@link Instant#EPOCH},
	 * in which case the minimum ID is ignored.
	 * <br>
	 * This method may be used to implement keyset pagination of selected punishments,
	 * together with {@link #limitToRetrieve(int)}. <br>
	 * <br>
	 * Behavior is unspecified if this is used simultaneously with {@link #skipFirstRetrieved(int)}.
	 * Callers should consider the methods exclusive to avoid unspecified behavior.
	 *
	 * @param minimumStartTime the minimum start time, or {@code Instant.EPOCH} to reset this option
	 * @param minimumId the minimum ID
	 * @return this builder
	 */
	SelectionOrderBuilder seekAfter(Instant minimumStartTime, long minimumId);

	/**
	 * Sets the time before which punishments will be retrieved. Only punishments whose
	 * start time ({@link Punishment#getStartDate()}) is before or equal to the given value will be
	 * selected. <br>
	 * <br>
	 * If a punishment has a start time equal to {@code maximumStartTime}, its ID will be checked against
	 * {@code maximumId}. The punishment will then be selected if its ID is equal to or less than
	 * {@code maximumId}. <br>
	 * <br>
	 * To reset this option to the default value, use a maximum start time of {@link Instant#EPOCH},
	 * in which case the maximum ID is ignored.
	 * <br>
	 * This method may be used to implement keyset pagination of selected punishments,
	 * together with {@link #limitToRetrieve(int)}. <br>
	 * <br>
	 * Behavior is unspecified if this is used simultaneously with {@link #skipFirstRetrieved(int)}.
	 * Callers should consider the methods exclusive to avoid unspecified behavior.
	 *
	 * @param maximumStartTime the maximum start time, or {@code Instant.EPOCH} to reset this option
	 * @param maximumId the maximum ID
	 * @return this builder
	 */
	SelectionOrderBuilder seekBefore(Instant maximumStartTime, long maximumId);

	/**
	 * Builds a {@link SelectionOrder} from the details of this builder. May be used
	 * repeatedly without side effects.
	 * 
	 * @return a selection order from this builder's details
	 * @throws IllegalStateException if the required details on this builder have not been set
	 */
	SelectionOrder build();

}
