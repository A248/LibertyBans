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

import java.util.Objects;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.PunishmentBase;

abstract class AbstractPunishmentBase implements PunishmentBase {
	
	transient final EnforcementCenter center;
	
	private final PunishmentType type;
	private final Victim victim;
	private final Operator operator;
	private final String reason;
	private final ServerScope scope;
	
	AbstractPunishmentBase(EnforcementCenter center,
			PunishmentType type, Victim victim, Operator operator, String reason, ServerScope scope) {
		this.center = center;

		this.type = Objects.requireNonNull(type, "type");
		this.victim = Objects.requireNonNull(victim, "victim");
		this.operator = Objects.requireNonNull(operator, "operator");
		this.reason = Objects.requireNonNull(reason, "reason");
		this.scope = Objects.requireNonNull(scope, "scope");
	}
	
	@Override
	public PunishmentType getType() {
		return type;
	}
	
	@Override
	public Victim getVictim() {
		return victim;
	}
	
	@Override
	public Operator getOperator() {
		return operator;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + type.hashCode();
		result = prime * result + victim.hashCode();
		result = prime * result + operator.hashCode();
		result = prime * result + reason.hashCode();
		result = prime * result + scope.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof AbstractPunishmentBase)) {
			return false;
		}
		AbstractPunishmentBase other = (AbstractPunishmentBase) object;
		return type == other.type
				&& victim.equals(other.victim)
				&& operator.equals(other.operator)
				&& reason.equals(other.reason)
				&& scope.equals(other.scope);
	}
	
}
