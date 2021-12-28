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
package space.arim.libertybans.api.punish;

import java.time.Duration;

import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;

/**
 * Builder of draft punishments. It is required to set the type, victim, and
 * reason.
 * 
 * @author A248
 *
 */
public interface DraftPunishmentBuilder {

	/**
	 * Sets the punishment type of this builder to the specified one. Required
	 * operation
	 * 
	 * @param type the type of the punishment
	 * @return this builder
	 */
	DraftPunishmentBuilder type(PunishmentType type);

	/**
	 * Sets the victim of this builder to the specified one. Required operation. <br>
	 * <br>
	 * If the victim is a {@code CompositeVictim}, it cannot use either {@link CompositeVictim#WILDCARD_UUID}
	 * or {@link CompositeVictim#WILDCARD_ADDRESS}
	 * 
	 * @param victim the victim of the punishmnt
	 * @return this builder
	 * @throws IllegalArgumentException if the victim is a {@code CompositeVictim} and uses either
	 * {@code WILDCARD_UUID} or {@code WILDCARD_ADDRESS}
	 */
	DraftPunishmentBuilder victim(Victim victim);

	/**
	 * Sets the operator of this builder to the specified one. If unspecified, the
	 * console operator is used ({@link ConsoleOperator#INSTANCE})
	 * 
	 * @param operator the operator of the punishment, by default the console
	 * @return this builder
	 */
	DraftPunishmentBuilder operator(Operator operator);

	/**
	 * Sets the reason of this builder to the specified one. Required operation
	 * 
	 * @param reason the reason of the punishment
	 * @return this builder
	 */
	DraftPunishmentBuilder reason(String reason);

	/**
	 * Sets the duration of this builder to the specified one. If unspecified, a
	 * duration of zero, indicating a permanent duration, is used
	 * 
	 * @param duration the duration of the punishment, or zero for a permanent
	 *                 punishment
	 * @return this builder
	 * @throws IllegalArgumentException if {@code duration} is negative
	 */
	DraftPunishmentBuilder duration(Duration duration);

	/**
	 * Sets the scope of this builder to the specified one. If unspecified, the
	 * global scope is used
	 * 
	 * @param scope the scope of the punishment
	 * @return this builder
	 */
	DraftPunishmentBuilder scope(ServerScope scope);

	/**
	 * Builds into a full draft punishment. All the required information must be set
	 * on this builder. <br>
	 * <br>
	 * May be used repeatedly without side effects.
	 * 
	 * @return a draft punishment from this builder's details
	 * @throws IllegalStateException    if any of the required details on this
	 *                                  builder are not set
	 * @throws IllegalArgumentException if the punishment type of this builder is
	 *                                  {@link PunishmentType#KICK} and the duration
	 *                                  is not permanent
	 */
	DraftPunishment build();

}
