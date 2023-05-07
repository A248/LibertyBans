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
 * A punishment ready to be created on the basis of dynamically computed details
 *
 */
public interface CalculablePunishment extends DraftSanction {

	/**
	 * Gets the calculator responsible for computing the extra details of the punishment
	 *
	 * @return the details calculator
	 */
	PunishmentDetailsCalculator getCalculator();

	/**
	 * Gets the escalation track according to which punishment details are computed
	 *
	 * @return the escalation track
	 */
	EscalationTrack getEscalationTrack();

	/**
	 * Creates a new {@link CalculablePunishmentBuilder}, copying all properties of this calculable.
	 *
	 * @return A new builder
	 */
	CalculablePunishmentBuilder toBuilder();

}
