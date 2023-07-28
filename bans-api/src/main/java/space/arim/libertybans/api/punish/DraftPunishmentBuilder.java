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

package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;

import java.time.Duration;

/**
 * Builder of draft punishments. It is required to set the type, victim, and
 * reason.
 *
 */
public interface DraftPunishmentBuilder extends DraftSanctionBuilder<DraftPunishmentBuilder, DraftPunishment> {

	/**
	 * Sets the punishment type of this builder to the specified one. Required
	 * operation
	 * 
	 * @param type the type of the punishment
	 * @return this builder
	 */
	DraftPunishmentBuilder type(PunishmentType type);

	/*
	Redeclare victim and operator methods from DraftSanctionBuilder for 1.0 API compatibility
	 */

	@Override
	DraftPunishmentBuilder victim(Victim victim);

	@Override
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
	 * Sets the escalation track of this builder. If unspecified, no escalation track is used.
	 *
	 * @param escalationTrack the escalation track, or {@code null} for none
	 * @return this builder
	 */
	DraftPunishmentBuilder escalationTrack(EscalationTrack escalationTrack);

	/**
	 * Builds into a full draft punishment. All the required information must be set on this builder. <br>
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
	// Redeclare from DraftSanctionBuilder for 1.0 API compatibility
	@Override
	DraftPunishment build();

}
