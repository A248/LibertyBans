/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.punish;

import java.time.Duration;
import java.util.Objects;

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.scope.ServerScope;

class DraftPunishmentBuilderImpl implements DraftPunishmentBuilder {

	private final Enactor enactor;
	
	PunishmentType type;
	Victim victim;
	Operator operator = ConsoleOperator.INSTANCE;
	String reason;
	ServerScope scope;
	Duration duration = Duration.ZERO;

	DraftPunishmentBuilderImpl(Enactor enactor) {
		this.enactor = enactor;
		scope = enactor.scopeManager().globalScope();
	}

	@Override
	public DraftPunishmentBuilder type(PunishmentType type) {
		this.type = Objects.requireNonNull(type, "type");
		return this;
	}

	@Override
	public DraftPunishmentBuilder victim(Victim victim) {
		MiscUtil.checkNoCompositeVictimWildcards(victim);
		this.victim = Objects.requireNonNull(victim, "victim");
		return this;
	}

	@Override
	public DraftPunishmentBuilder operator(Operator operator) {
		this.operator = Objects.requireNonNull(operator, "operator");
		return this;
	}

	@Override
	public DraftPunishmentBuilder reason(String reason) {
		this.reason = Objects.requireNonNull(reason, "reason");
		return this;
	}

	@Override
	public DraftPunishmentBuilder scope(ServerScope scope) {
		enactor.scopeManager().checkScope(scope);
		this.scope = scope;
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
