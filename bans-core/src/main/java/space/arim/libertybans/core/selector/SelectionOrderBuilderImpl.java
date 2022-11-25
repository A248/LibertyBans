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
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.api.select.SelectionPredicate;

import java.time.Instant;
import java.util.Objects;

class SelectionOrderBuilderImpl implements SelectionOrderBuilder {

	private final SelectorImpl selector;

	private SelectionPredicate<PunishmentType> types = SelectionPredicate.matchingAll();
	private SelectionPredicate<Victim> victims = SelectionPredicate.matchingAll();
	private SelectionPredicate<Victim.VictimType> victimTypes = SelectionPredicate.matchingAll();
	private SelectionPredicate<Operator> operators = SelectionPredicate.matchingAll();
	private SelectionPredicate<ServerScope> scopes = SelectionPredicate.matchingAll();
	private boolean selectActiveOnly = true;
	private int skipCount;
	private int limitToRetrieve;
	private Instant seekAfterStartTime = Instant.EPOCH;
	private long seekAfterId;
	private Instant seekBeforeStartTime = Instant.EPOCH;
	private long seekBeforeId;

	SelectionOrderBuilderImpl(SelectorImpl selector) {
		this.selector = selector;
	}

	@Override
	public SelectionOrderBuilder types(SelectionPredicate<PunishmentType> types) {
		this.types = Objects.requireNonNull(types, "types");
		return this;
	}

	@Override
	public SelectionOrderBuilder victims(SelectionPredicate<Victim> victims) {
		this.victims = Objects.requireNonNull(victims, "victims");
		return this;
	}

	@Override
	public SelectionOrderBuilder victimTypes(SelectionPredicate<Victim.VictimType> victimTypes) {
		this.victimTypes = Objects.requireNonNull(victimTypes, "victimTypes");
		return this;
	}

	@Override
	public SelectionOrderBuilder operators(SelectionPredicate<Operator> operators) {
		this.operators = Objects.requireNonNull(operators, "operator");
		return this;
	}

	@Override
	public SelectionOrderBuilder scopes(SelectionPredicate<ServerScope> scopes) {
		this.scopes = Objects.requireNonNull(scopes, "scopes");
		return this;
	}

	@Override
	public SelectionOrderBuilder selectActiveOnly(boolean selectActiveOnly) {
		this.selectActiveOnly = selectActiveOnly;
		return this;
	}

	@Override
	public SelectionOrderBuilder skipFirstRetrieved(int skipCount) {
		if (skipCount < 0) {
			throw new IllegalArgumentException("Skip count must be non-negative");
		}
		this.skipCount = skipCount;
		return this;
	}

	@Override
	public SelectionOrderBuilder limitToRetrieve(int limitToRetrieve) {
		if (limitToRetrieve < 0) {
			throw new IllegalArgumentException("Maximum to retrieve must be non-negative");
		}
		this.limitToRetrieve = limitToRetrieve;
		return this;
	}

	@Override
	public SelectionOrderBuilder seekAfter(Instant minimumStartTime, long minimumId) {
		this.seekAfterStartTime = Objects.requireNonNull(minimumStartTime, "minimumStartTime");
		this.seekAfterId = minimumId;
		return this;
	}

	@Override
	public SelectionOrderBuilder seekBefore(Instant maximumStartTime, long maximumId) {
		this.seekBeforeStartTime = Objects.requireNonNull(maximumStartTime, "maximumStartTime");
		this.seekBeforeId = maximumId;
		return this;
	}

	@Override
	public SelectionOrder build() {
		return new SelectionOrderImpl(selector,
				types, victims, victimTypes, operators, scopes,
				selectActiveOnly, skipCount, limitToRetrieve,
				seekAfterStartTime, seekAfterId, seekBeforeStartTime, seekBeforeId
		);
	}
	
}
