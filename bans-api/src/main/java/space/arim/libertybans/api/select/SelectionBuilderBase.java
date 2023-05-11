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

package space.arim.libertybans.api.select;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;

import java.time.Instant;
import java.util.Optional;

/**
 * Base interface for the builder of a selection.
 *
 * @param <B> the specific kind of this builder
 * @param <S> the specific kind of the corresponding selection
 */
public interface SelectionBuilderBase<B extends SelectionBuilderBase<B, S>, S extends SelectionBase> {

	/**
	 * Sets the punishment type matched. All are matched by default.
	 *
	 * @param type the punishment type
	 * @return this builder
	 */
	default B type(PunishmentType type) {
		return types(SelectionPredicate.matchingOnly(type));
	}

	/**
	 * Sets the punishment types matched. All are matched by default.
	 *
	 * @param types the punishment type predicate
	 * @return this builder
	 */
	B types(SelectionPredicate<PunishmentType> types);

	/**
	 * Sets the operator matched. All are matched by default.
	 *
	 * @param operator the operator
	 * @return this builder
	 */
	default B operator(Operator operator) {
		return operators(SelectionPredicate.matchingOnly(operator));
	}

	/**
	 * Sets the operators matched. All are matched by default.
	 *
	 * @param operators the operator predicate
	 * @return this builder
	 */
	B operators(SelectionPredicate<Operator> operators);

	/**
	 * Sets the scope matched. All are matched by default.
	 *
	 * @param scope the scope
	 * @return this builder
	 */
	default B scope(ServerScope scope) {
		return scopes(SelectionPredicate.matchingOnly(scope));
	}

	/**
	 * Sets the scopes matched. All are matched by default.
	 *
	 * @param scopes the scope predicate
	 * @return this builder
	 */
	B scopes(SelectionPredicate<ServerScope> scopes);

	/**
	 * Sets the escalation track matched. All are matched by default.
	 *
	 * @param escalationTrack the escalation track, or null to match the inexistence of a track
	 * @return this builder
	 */
	default B escalationTrack(EscalationTrack escalationTrack) {
		return escalationTracks(SelectionPredicate.matchingOnly(Optional.ofNullable(escalationTrack)));
	}

	/**
	 * Sets the escalation tracks matched. All are matched by default. An empty optional indicates
	 * the inexistence of a track
	 *
	 * @param escalationTracks the escalation track predicate
	 * @return this builder
	 */
	B escalationTracks(SelectionPredicate<Optional<EscalationTrack>> escalationTracks);

	/**
	 * Sets whether only active punishments should be matched. True by default,
	 * meaning only active punishments are selected. <br>
	 * <br>
	 * Active punishments are those not expired and not undone.
	 *
	 * @param selectActiveOnly whether to select active punishments only
	 * @return this builder
	 */
	B selectActiveOnly(boolean selectActiveOnly);

	/**
	 * Sets that only active punishments should be matched. Enabled by default,
	 * meaning only active punishments are selected. <br>
	 * <br>
	 * Active punishments are those not expired and not undone.
	 *
	 * @return this builder
	 */
	default B selectActiveOnly() {
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
	default B selectAll() {
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
	B skipFirstRetrieved(int skipCount);

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
	B limitToRetrieve(int limitToRetrieve);

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
	B seekAfter(Instant minimumStartTime, long minimumId);

	/**
	 * Sets the time before which punishments will be retrieved. Only punishments whose
	 * start time ({@link Punishment#getStartDate()}) is before or equal to the given value will be
	 * selected. <br>
	 * <br>
	 * If a punishment has a start time equal to {@code maximumStartTime}, its ID will be checked against
	 * {@code maximumId}. The punishment will then be selected if its ID is equal to or less than
	 * {@code maximumId}. <br>
	 * <br>
	 * To reset this option to the default value, use a maximum start time of {@link Instant#MAX},
	 * in which case the maximum ID is ignored.
	 * <br>
	 * This method may be used to implement keyset pagination of selected punishments,
	 * together with {@link #limitToRetrieve(int)}. <br>
	 * <br>
	 * Behavior is unspecified if this is used simultaneously with {@link #skipFirstRetrieved(int)}.
	 * Callers should consider the methods exclusive to avoid unspecified behavior.
	 *
	 * @param maximumStartTime the maximum start time, or {@code Instant.MAX} to reset this option
	 * @param maximumId the maximum ID
	 * @return this builder
	 */
	B seekBefore(Instant maximumStartTime, long maximumId);

	/**
	 * Builds a selection from the details of this builder. May be used repeatedly without side effects.
	 *
	 * @return a selection from this builder's details
	 * @throws IllegalStateException if the required details on this builder have not been set
	 */
	S build();

}
