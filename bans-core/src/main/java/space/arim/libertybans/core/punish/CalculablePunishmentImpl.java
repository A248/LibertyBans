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
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDetailsCalculator;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class CalculablePunishmentImpl extends AbstractSanctionBase implements CalculablePunishment, EnforcementOpts.Factory {

	private final Enactor enactor;
	private final PunishmentDetailsCalculator calculator;
	private final EscalationTrack escalationTrack;

	CalculablePunishmentImpl(Enactor enactor, CalculablePunishmentBuilderImpl builder) {
		super(builder.victim, builder.operator);
		this.enactor = enactor;
		calculator = builder.calculator;
		escalationTrack = builder.escalationTrack;
	}

	@Override
	public PunishmentDetailsCalculator getCalculator() {
		return calculator;
	}

	@Override
	public EscalationTrack getEscalationTrack() {
		return escalationTrack;
	}

	@Override
	public CalculablePunishmentBuilder toBuilder() {
		return enactor.calculablePunishmentBuilder()
				.victim(getVictim())
				.operator(getOperator())
				.calculator(getCalculator())
				.escalationTrack(getEscalationTrack());
	}

	@Override
	public ReactionStage<Optional<Punishment>> enactPunishment(EnforcementOptions enforcementOptions) {
		return enactor.calculatePunishment(this).thenCompose((punishment) -> {
			if (punishment == null) {
				return CompletableFuture.completedStage(Optional.empty());
			}
			return punishment.enforcePunishment(enforcementOptions).thenApply((ignore) -> Optional.of(punishment));
		});
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		CalculablePunishmentImpl that = (CalculablePunishmentImpl) o;
		return calculator.equals(that.calculator) && Objects.equals(escalationTrack, that.escalationTrack);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + calculator.hashCode();
		result = 31 * result + (escalationTrack != null ? escalationTrack.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "CalculablePunishmentImpl{" +
				"calculator=" + calculator +
				", track=" + escalationTrack +
				", victim=" + getVictim() +
				", operator=" + getOperator() +
				'}';
	}

}
