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

package space.arim.libertybans.core.addon.layouts;

import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.PunishmentDetailsCalculator;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.SelectionOrderBuilder;

record TrackCalculator(ScopeManager scopeManager, Track.Ladder ladder) implements PunishmentDetailsCalculator {

	@Override
	public CalculationResult compute(EscalationTrack escalationTrack, Victim victim,
									 SelectionOrderBuilder selectionOrderBuilder) {
		Integer existingPunishments = selectionOrderBuilder
				.victim(victim)
				.escalationTrack(escalationTrack)
				.selectActiveOnly(ladder.countActive())
				.build()
				.countNumberOfPunishments()
				.toCompletableFuture()
				.getNow(null);
		if (existingPunishments == null) {
			throw new IllegalStateException("API implementation error: future incomplete");
		}
		var progression = findProgression(existingPunishments + 1);
		return new PunishmentDetailsCalculator.CalculationResult(
				progression.type(), progression.reason(),
				progression.duration().duration(), progression.scope().actualize(scopeManager)
		);
	}

	private Track.Ladder.Progression findProgression(int index) {
		var progressions = ladder.progressions();
		for (int n = index; n > 0; n--) {
			var progression = progressions.get(n);
			if (progression != null) {
				return progression;
			}
		}
		throw new IllegalStateException("No progression applicable");
	}

}
