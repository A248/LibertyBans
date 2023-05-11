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

import space.arim.libertybans.api.scope.ServerScope;

import java.time.Duration;
import java.time.Instant;

/**
 * Assistant interface for modifying a punishment
 *
 */
public interface PunishmentEditor {

	/**
	 * Sets the new reason
	 *
	 * @param reason the new reason
	 */
	void setReason(String reason);

	/**
	 * Sets the new scope
	 *
	 * @param scope the new scope
	 */
	void setScope(ServerScope scope);

	/**
	 * Sets the new end date. {@link Punishment#PERMANENT_END_DATE} is used for a
	 * permanent punishment
	 *
	 * @param endDate the new end date
	 * @throws IllegalStateException if {@link #extendEndDate(Duration)} was used
	 */
	void setEndDate(Instant endDate);

	/**
	 * Modifies the end date by adding the given duration. <br>
	 * <br>
	 * It is possible to reduce the end date by using a negative duration. Note that if
	 * the subtraction operation would otherwise cause the punishment to have an end date less
	 * than or equal to the start date, then 1 second plus the start date is used as the new
	 * end date.
	 *
	 * @param endDateDelta the amount by which to change the end date, may be negative
	 * @throws IllegalStateException if {@link #setEndDate(Instant)} was used
	 */
	void extendEndDate(Duration endDateDelta);

	/**
	 * Sets the new escalation track, or {@code null} for none
	 *
	 * @param escalationTrack the new escalation track or null to clear the track
	 */
	void setEscalationTrack(EscalationTrack escalationTrack);

}
