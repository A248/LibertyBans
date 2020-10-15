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

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionOrderBuilder;

class SelectionOrderBuilderImpl implements SelectionOrderBuilder {
	
	private final Selector selector;
	
	private PunishmentType type;
	private Victim victim;
	private Operator operator;
	private ServerScope scope;
	private boolean selectActiveOnly = true;
	
	SelectionOrderBuilderImpl(Selector selector) {
		this.selector = selector;
	}

	@Override
	public SelectionOrderBuilder type(PunishmentType type) {
		this.type = type;
		return this;
	}

	@Override
	public SelectionOrderBuilder victim(Victim victim) {
		this.victim = victim;
		return this;
	}

	@Override
	public SelectionOrderBuilder operator(Operator operator) {
		this.operator = operator;
		return this;
	}

	@Override
	public SelectionOrderBuilder scope(ServerScope scope) {
		this.scope = scope;
		return this;
	}

	@Override
	public SelectionOrderBuilder selectActiveOnly(boolean selectActiveOnly) {
		this.selectActiveOnly = selectActiveOnly;
		return this;
	}

	@Override
	public SelectionOrder build() {
		return new SelectionOrderImpl(selector, type, victim, operator, scope, selectActiveOnly);
	}
	
}
