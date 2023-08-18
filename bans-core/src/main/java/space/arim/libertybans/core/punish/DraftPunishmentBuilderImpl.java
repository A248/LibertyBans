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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.scope.ServerScope;

import java.time.Duration;
import java.util.Objects;

class DraftPunishmentBuilderImpl extends DraftSanctionBuilderImpl<DraftPunishmentBuilder, DraftPunishment>
		implements DraftPunishmentBuilder {

	private final Enactor enactor;

	PunishmentType type;
	String reason;
	ServerScope scope;
	EscalationTrack escalationTrack;
	Duration duration = Duration.ZERO;

	DraftPunishmentBuilderImpl(Enactor enactor) {
		this.enactor = enactor;
		scope = enactor.scopeManager().globalScope();
	}

	@Override
	DraftPunishmentBuilder yieldSelf() {
		return this;
	}

	@Override
	public DraftPunishmentBuilder type(PunishmentType type) {
		this.type = Objects.requireNonNull(type, "type");
		return this;
	}

	@Override
	public DraftPunishmentBuilder reason(String reason) {
		this.reason = Objects.requireNonNull(reason, "reason");
		return this;
	}

	@Override
	public DraftPunishmentBuilder scope(ServerScope scope) {
		this.scope = enactor.scopeManager().checkScope(scope);
		return this;
	}

	@Override
	public DraftPunishmentBuilder escalationTrack(EscalationTrack escalationTrack) {
		this.escalationTrack = escalationTrack;
		return this;
	}

	@Override
	public DraftPunishmentBuilder duration(Duration duration) {
		Objects.requireNonNull(duration, "duration");
		if (duration.isNegative()) {
			throw new IllegalArgumentException("duration cannot be negative");
		}
		this.duration = duration;
		return this;
	}

	@Override
	public DraftPunishment build() {
		if (type == null || victim == null || reason == null) {
			throw new IllegalStateException("Builder details have not been set");
		}
		if (type == PunishmentType.KICK && !duration.isZero()) {
			throw new IllegalArgumentException("Kicks cannot be temporary");
		}
		return new DraftPunishmentImpl(enactor, this);
	}

}
