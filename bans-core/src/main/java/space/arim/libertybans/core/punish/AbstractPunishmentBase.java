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

import java.util.Objects;
import java.util.Optional;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.PunishmentBase;
import space.arim.libertybans.api.scope.ServerScope;

abstract class AbstractPunishmentBase extends AbstractSanctionBase implements PunishmentBase {
	
	private final PunishmentType type;
	private final String reason;
	private final ServerScope scope;
	private final EscalationTrack escalationTrack;

	AbstractPunishmentBase(PunishmentType type, Victim victim, Operator operator, String reason,
						   ServerScope scope, EscalationTrack escalationTrack) {
		super(victim, operator);
		this.type = Objects.requireNonNull(type, "type");
		this.reason = Objects.requireNonNull(reason, "reason");
		this.scope = Objects.requireNonNull(scope, "scope");
		this.escalationTrack = escalationTrack;
	}

	@Override
	public PunishmentType getType() {
		return type;
	}

	@Override
	public String getReason() {
		return reason;
	}

	@Override
	public ServerScope getScope() {
		return scope;
	}

	@Override
	public Optional<EscalationTrack> getEscalationTrack() {
		return Optional.ofNullable(escalationTrack);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		AbstractPunishmentBase that = (AbstractPunishmentBase) o;
		return type == that.type && reason.equals(that.reason) && scope.equals(that.scope)
				&& Objects.equals(escalationTrack, that.escalationTrack);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + reason.hashCode();
		result = 31 * result + scope.hashCode();
		result = 31 * result + (escalationTrack != null ? escalationTrack.hashCode() : 0);
		return result;
	}

}
