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
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A selection which will match punishments in the database with certain details.
 * <br>
 * A punishment is said to match if it meets all of this selection's requirements.
 *
 */
public interface SelectionBase {

	/**
	 * Gets the punishment types matched
	 *
	 * @return the punishment types
	 */
	SelectionPredicate<PunishmentType> getTypes();

	/**
	 * Gets the operators matched
	 *
	 * @return the operators
	 */
	SelectionPredicate<Operator> getOperators();

	/**
	 * Gets the scopes matched
	 *
	 * @return the scopes
	 */
	SelectionPredicate<ServerScope> getScopes();

	/**
	 * Gets the escalation tracks matched
	 *
	 * @return the escalation tracks
	 */
	SelectionPredicate<Optional<EscalationTrack>> getEscalationTracks();

	/**
	 * Whether this selection will match only active, non-expired punishments. If
	 * {@code false}, then this selection may match any punishments, including
	 * undone or expired punishments.
	 *
	 * @return true to select only active and non-expired punishments, false to
	 *         select all punishments
	 */
	boolean selectActiveOnly();

	/**
	 * Gets the initial amount of punishments skipped. <br>
	 * <br>
	 * See {@link SelectionBuilderBase#skipFirstRetrieved(int)}
	 *
	 * @return the amount of punishments skipped
	 */
	int skipCount();

	/**
	 * Gets the amount of punishments to retrieve. <br>
	 * <br>
	 * See {@link SelectionBuilderBase#limitToRetrieve(int)}
	 *
	 * @return the amount to retrieve, or {@code 0} for unlimited
	 */
	int limitToRetrieve();

	/**
	 * Gets the minimum start time after which punishments will be selected. <br>
	 * <br>
	 * See {@link SelectionBuilderBase#seekAfter(Instant, long)}
	 *
	 * @return the minimum start time, or {@link Instant#EPOCH} for none
	 */
	Instant seekAfterStartTime();

	/**
	 * Gets the minimum ID after which punishments will be selected. <br>
	 * <br>
	 * See {@link SelectionBuilderBase#seekAfter(Instant, long)}
	 *
	 * @return the minimum ID, which is meaningless if {@link #seekAfterStartTime()} is {@code Instant.EPOCH}
	 */
	long seekAfterId();

	/**
	 * Gets the maximum start time before which punishments will be selected. <br>
	 * <br>
	 * See {@link SelectionBuilderBase#seekBefore(Instant, long)}
	 *
	 * @return the maximum start time, or {@link Instant#MAX} for none
	 */
	Instant seekBeforeStartTime();

	/**
	 * Gets the maximum ID before which punishments will be selected. <br>
	 * <br>
	 * See {@link SelectionBuilderBase#seekBefore(Instant, long)}
	 *
	 * @return the maximum ID, which is meaningless if {@link #seekBeforeStartTime()} is {@code Instant.MAX}
	 */
	long seekBeforeId();

	/**
	 * Gets the first punishment matching this selection. <br>
	 * <br>
	 * If multiple punishments exist, the one chosen depends on the date enacted.
	 * The latest punishment will come first.
	 *
	 * @return a future which yields the first punishment matching this selection,
	 *         or an empty optional if none matched
	 */
	default ReactionStage<Optional<Punishment>> getFirstSpecificPunishment() {
		return getFirstSpecificPunishment(SortPunishments.NEWEST_FIRST);
	}

	/**
	 * Gets the first punishment matching this selection. <br>
	 * <br>
	 * If multiple punishments exist, the {@code prioritization} parameter determines which
	 * will be returned.
	 *
	 * @param prioritization how to evaluate which punishment comes first
	 * @return a future which yields the first punishment matching this selection,
	 *         or an empty optional if none matched
	 */
	ReactionStage<Optional<Punishment>> getFirstSpecificPunishment(SortPunishments...prioritization);

	/**
	 * Gets all punishments matching this selection. <br>
	 * <br>
	 * The returned list is ordered by the date the punishment was enacted.
	 * The latest punishments come first.
	 *
	 * @return a future which yields all punishments matching this selection, or an
	 *         empty set if none matched
	 */
	default ReactionStage<List<Punishment>> getAllSpecificPunishments() {
		return getAllSpecificPunishments(SortPunishments.NEWEST_FIRST);
	}

	/**
	 * Gets all punishments matching this selection, ordered in the specified fashion. <br>
	 * <br>
	 * Using the {@code ordering} parameter is potentially superior to manually sorting the
	 * returned list, since it may perform ordering on the database side.
	 *
	 * @param ordering how to order the returned punishments
	 * @return a future which yields all punishments matching this selection, or an
	 *         empty set if none matched
	 */
	ReactionStage<List<Punishment>> getAllSpecificPunishments(SortPunishments...ordering);

	/**
	 * Gets the number of punishments matching this selection.
	 *
	 * @return a future which yields the number of punishments matching this selection
	 */
	ReactionStage<Integer> countNumberOfPunishments();

	/**
	 * Whether this punishment selection is equal to another, i.e. if the other
	 * selection would match the same punishments in all circumstances.
	 *
	 * @param object the other object
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	boolean equals(Object object);

}
