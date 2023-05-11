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

/**
 * Builder of calculable punishments. It is required to set the victim, calculator, and escalation track.
 *
 */
public interface CalculablePunishmentBuilder extends DraftSanctionBuilder<CalculablePunishmentBuilder, CalculablePunishment> {

	/**
	 * Sets the calculator of this builder to the specified one
	 *
	 * @param calculator the calculator to use
	 * @return this builder
	 */
	CalculablePunishmentBuilder calculator(PunishmentDetailsCalculator calculator);

	/**
	 * Sets the escalation track of this builder to the specified one
	 *
	 * @param escalationTrack the escalation track
	 * @return this builder
	 */
	CalculablePunishmentBuilder escalationTrack(EscalationTrack escalationTrack);

}
