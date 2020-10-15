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
package space.arim.libertybans.core.selector;

import java.util.Objects;
import java.util.Set;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.SelectionOrder;

class SelectionOrderImpl implements SelectionOrder {
	
	private transient final Selector selector;
	
	private final PunishmentType type;
	private final Victim victim;
	private final Operator operator;
	private final ServerScope scope;
	private final boolean selectActiveOnly;
	
	SelectionOrderImpl(Selector selector,
			PunishmentType type, Victim victim, Operator operator, ServerScope scope, boolean selectActiveOnly) {
		this.selector = selector;

		this.type = type;
		this.victim = victim;
		this.operator = operator;
		this.scope = scope;
		this.selectActiveOnly = selectActiveOnly;
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
	public ServerScope getScope() {
		return scope;
	}
	
	@Override
	public boolean selectActiveOnly() {
		return selectActiveOnly;
	}
	
	@Override
	public CentralisedFuture<Punishment> getFirstSpecificPunishment() {
		return selector.getFirstSpecificPunishment(this);
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getAllSpecificPunishments() {
		return selector.getSpecificPunishments(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(type);
		result = prime * result + Objects.hashCode(victim);
		result = prime * result + Objects.hashCode(operator);
		result = prime * result + Objects.hashCode(scope);
		result = prime * result + (selectActiveOnly ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof SelectionOrderImpl)) {
			return false;
		}
		SelectionOrderImpl other = (SelectionOrderImpl) object;
		return type == other.type
				&& Objects.equals(victim, other.victim)
				&& Objects.equals(operator, other.operator)
				&& Objects.equals(scope, other.scope)
				&& selectActiveOnly == other.selectActiveOnly;
	}

	@Override
	public String toString() {
		return "SelectionOrderImpl [type=" + type + ", victim=" + victim + ", operator=" + operator + ", scope=" + scope
				+ ", selectActiveOnly=" + selectActiveOnly + "]";
	}
	
}
