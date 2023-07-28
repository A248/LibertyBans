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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionOrderBuilder;

import java.time.Duration;
import java.util.Objects;

/**
 * Client interface implemented in order to provide calculation of punishment details
 * within the scope of a database transaction.
 *
 */
public interface PunishmentDetailsCalculator {

	/**
	 * Computes punishment details for the given victim. <br>
	 * <br>
	 * This method is typically called during the scope of a database transaction, so it should be fast and responsive.
	 * The implementation should be stateless, as it may be retried in the case of transaction serialization failure.
	 * <br> <br>
	 * Most implementations will wish to retrieve punishments for the specified victim. Punishment retrieval should be
	 * performed through and only through the provided selection order builder. A special implementation of the
	 * selection interfaces is utilized which completes database operations immediately using the same transaction
	 * within which this method is called.
	 *
	 * @param track the escalation track according to which to compute
	 * @param victim the victim being punished
	 * @param selectionOrderBuilder the selection order builder for which to retrieve punishments in the same context
	 *                                 as this calculator is called. It is guaranteed that all futures returned from
	 *                                 any built selection orders are completed immediately
	 * @return the calculated punishment details
	 * @throws IllegalArgumentException optionally, if this calculator does not support the specified escalation track
	 */
	CalculationResult compute(EscalationTrack track, Victim victim, SelectionOrderBuilder selectionOrderBuilder);

	/**
	 * The computed details of a punishment
	 *
	 * @param type the type
	 * @param reason the reason
	 * @param duration the duration of the punishment starting now, {@code Duration.ZERO} for permanent
	 * @param scope the scope
	 */
	record CalculationResult(PunishmentType type, String reason, Duration duration, ServerScope scope) {

		/**
		 * Creates
		 *
		 * @param type the type
		 * @param reason the reason
		 * @param duration the duration of the punishment starting now, {@code Duration.ZERO} for permanent
		 * @param scope the scope
		 * @throws IllegalArgumentException if {@code duration} is negative, or if the punishment type is
		 * {@link PunishmentType#KICK} but the duration is not permanent
		 */
		public CalculationResult {
			Objects.requireNonNull(type, "type");
			Objects.requireNonNull(reason, "reason");
			Objects.requireNonNull(duration, "duration");
			Objects.requireNonNull(scope, "scope");
			if (duration.isNegative()) {
				throw new IllegalArgumentException("duration cannot be negative");
			}
			if (type == PunishmentType.KICK && !duration.isZero()) {
				throw new IllegalArgumentException("Kicks cannot be temporary");
			}
		}

	}

}
