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
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A selection which will match punishments in the database with certain
 * details. <br>
 * <br>
 * To retrieve an instance, use the {@link SelectionOrderBuilder} obtained from
 * {@link PunishmentSelector#selectionBuilder()}
 * 
 * @author A248
 *
 */
public interface SelectionOrder {

	/**
	 * Gets the punishment types matched
	 * 
	 * @return the punishment types
	 */
	SelectionPredicate<PunishmentType> getTypes();

	/**
	 * Gets the victims matched
	 * 
	 * @return the victims
	 */
	SelectionPredicate<Victim> getVictims();

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
	 * See {@link SelectionOrderBuilder#skipFirstRetrieved(int)}
	 * 
	 * @return the amount of punishments skipped
	 */
	int skipCount();

	/**
	 * Gets the amount of punishments to retrieve. <br>
	 * <br>
	 * See {@link SelectionOrderBuilder#limitToRetrieve(int)}
	 * 
	 * @return the amount to retrieve, or {@code 0} for unlimited
	 */
	int limitToRetrieve();

	/**
	 * Gets the minimum start time after which punishments will be selected. <br>
	 * <br>
	 * See {@link SelectionOrderBuilder#seekAfter(Instant, long)}
	 *
	 * @return the minimum start time, or {@link Instant#EPOCH} for none
	 */
	Instant seekAfterStartTime();

	/**
	 * Gets the minimum ID after which punishments will be selected. <br>
	 * <br>
	 * See {@link SelectionOrderBuilder#seekAfter(Instant, long)}
	 *
	 * @return the minimum ID, which is meaningless if {@link #seekAfterStartTime()} is {@code Instant.EPOCH}
	 */
	long seekAfterId();

	Instant seekBeforeStartTime();

	long seekBeforeId();

	/**
	 * Gets the first punishment matching this selection, i.e. with the specified
	 * details. <br>
	 * <br>
	 * A punishment is said to match if it meets ALL of the following: <br>
	 * Its type matches that of this selection <br>
	 * Its victim matches that of this selection, or this selection's victim is
	 * unspecified <br>
	 * Its operator matches that of this selection, or this selection's operator is
	 * unspecified <br>
	 * Its scope matches that of this selection, or this selection's scope is
	 * unspecified
	 * 
	 * @return a future which yields the first punishment matching this selection,
	 *         or an empty optional if none matched
	 */
	ReactionStage<Optional<Punishment>> getFirstSpecificPunishment();

	/**
	 * Gets all punishments matching the given punishment selection, i.e. with the
	 * specified details. <br>
	 * <br>
	 * The returned list is ordered by the date the punishment was enacted. The
	 * latest punishments come first. <br>
	 * <br>
	 * A punishment is said to match if it meets ALL of the following: <br>
	 * Its type matches that of this selection <br>
	 * Its victim matches that of this selection, or this selection's victim is
	 * unspecified <br>
	 * Its operator matches that of this selection, or this selection's operator is
	 * unspecified <br>
	 * Its scope matches that of this selection, or this selection's scope is
	 * unspecified
	 * 
	 * @return a future which yields all punishments matching this selection, or an
	 *         empty set if none matched
	 */
	ReactionStage<List<Punishment>> getAllSpecificPunishments();

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
