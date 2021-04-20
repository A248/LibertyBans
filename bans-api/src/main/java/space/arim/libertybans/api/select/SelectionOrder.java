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

import java.util.List;
import java.util.Optional;

import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;

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
	 * Gets the punishment type matched, or none to match all types
	 * 
	 * @return the punishment type, or none for all types
	 */
	Optional<PunishmentType> getType();

	/**
	 * Gets the victim matched, or none to match all victims
	 * 
	 * @return the victim or none for all victims
	 */
	Optional<Victim> getVictim();

	/**
	 * Gets the operator matched, or none to match all operators
	 * 
	 * @return the operator or none for all operators
	 */
	Optional<Operator> getOperator();

	/**
	 * Gets the scope matched, or none to match all scopes
	 * 
	 * @return the scope or none for all scopes
	 */
	Optional<ServerScope> getScope();

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
	 * Gets the maximum amount of punishments to retrieve. <br>
	 * <br>
	 * See {@link SelectionOrderBuilder#maximumToRetrieve(int)}
	 * 
	 * @return the maximum amount to retrieve, or {@code 0} for unlimited
	 */
	int maximumToRetrieve();

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
