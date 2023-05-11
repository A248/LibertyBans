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

package space.arim.libertybans.core.selector;

import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.api.select.SelectionPredicate;

import java.util.Objects;

final class SelectionOrderBuilderImpl extends SelectionBuilderBaseImpl<SelectionOrderBuilder, SelectionOrder>
		implements SelectionOrderBuilder {

	private final SelectionResources resources;
	private SelectionPredicate<Victim> victims = SelectionPredicate.matchingAll();
	private SelectionPredicate<Victim.VictimType> victimTypes = SelectionPredicate.matchingAll();

	SelectionOrderBuilderImpl(SelectionResources resources) {
		this.resources = resources;
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
	SelectionOrderBuilder yieldSelf() {
		return this;
	}

	@Override
	SelectionOrder buildWith(SelectionBaseImpl.Details details) {
		return new SelectionOrderImpl(
				details, resources, victims, victimTypes
		);
	}

}
