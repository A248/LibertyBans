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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;

class DraftPunishmentImpl extends AbstractPunishmentBase implements DraftPunishment, EnforcementOpts.Factory {

	private final Enactor enactor;
	private final Duration duration;

	DraftPunishmentImpl(Enactor enactor, DraftPunishmentBuilderImpl builder) {
		super(builder.type, builder.victim, builder.operator, builder.reason, builder.scope, builder.escalationTrack);
		this.enactor = enactor;
		duration = builder.duration;
	}

	@Override
	public Duration getDuration() {
		return duration;
	}

	@Override
	public ReactionStage<Optional<Punishment>> enactPunishment(EnforcementOptions enforcementOptions) {
		return enactor.enactPunishment(this).thenCompose((punishment) -> {
			if (punishment == null) {
				return CompletableFuture.completedStage(Optional.empty());
			}
			return punishment.enforcePunishment(enforcementOptions).thenApply((ignore) -> Optional.of(punishment));
		});
	}

	@Override
	public DraftPunishmentBuilder toBuilder() {
		return enactor.draftBuilder()
				.type(getType())
				.victim(getVictim())
				.operator(getOperator())
				.reason(getReason())
				.duration(getDuration())
				.scope(getScope())
				.escalationTrack(getEscalationTrack().orElse(null));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + duration.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!super.equals(object)) {
			return false;
		}
		if (!(object instanceof DraftPunishmentImpl other)) {
			return false;
		}
		return duration.equals(other.duration);
	}

	@Override
	public String toString() {
		return "DraftPunishmentImpl{" +
				"type=" + getType() +
				", victim=" + getVictim() +
				", operator=" + getOperator() +
				", reason='" + getReason() + '\'' +
				", scope=" + getScope() +
				", duration=" + duration +
				", track=" + getEscalationTrack() +
				'}';
	}

}
