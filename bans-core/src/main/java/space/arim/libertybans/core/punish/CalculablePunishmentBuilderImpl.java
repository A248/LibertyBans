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

package space.arim.libertybans.core.punish;

import space.arim.libertybans.api.punish.CalculablePunishment;
import space.arim.libertybans.api.punish.CalculablePunishmentBuilder;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.PunishmentDetailsCalculator;

import java.util.Objects;

class CalculablePunishmentBuilderImpl extends DraftSanctionBuilderImpl<CalculablePunishmentBuilder, CalculablePunishment> implements CalculablePunishmentBuilder {

	private final Enactor enactor;

	PunishmentDetailsCalculator calculator;
	EscalationTrack escalationTrack;

	CalculablePunishmentBuilderImpl(Enactor enactor) {
		this.enactor = enactor;
	}

	@Override
	CalculablePunishmentBuilder yieldSelf() {
		return this;
	}

	@Override
	public CalculablePunishmentBuilder calculator(PunishmentDetailsCalculator calculator) {
		this.calculator = Objects.requireNonNull(calculator, "calculator");
		return this;
	}

	@Override
	public CalculablePunishmentBuilder escalationTrack(EscalationTrack escalationTrack) {
		this.escalationTrack = Objects.requireNonNull(escalationTrack, "track");
		return this;
	}

	@Override
	public CalculablePunishment build() {
		if (victim == null || calculator == null || escalationTrack == null) {
			throw new IllegalStateException("Builder details have not been set");
		}
		return new CalculablePunishmentImpl(enactor, this);
	}

}
