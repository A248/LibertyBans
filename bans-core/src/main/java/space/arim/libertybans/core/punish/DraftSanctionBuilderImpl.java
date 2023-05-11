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

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftSanction;
import space.arim.libertybans.api.punish.DraftSanctionBuilder;

import java.util.Objects;

abstract class DraftSanctionBuilderImpl<B extends DraftSanctionBuilder<B, D>, D extends DraftSanction>
		implements DraftSanctionBuilder<B, D> {

	Victim victim;
	Operator operator = ConsoleOperator.INSTANCE;

	abstract B yieldSelf();

	@Override
	public B victim(Victim victim) {
		MiscUtil.checkNoCompositeVictimWildcards(victim);
		this.victim = Objects.requireNonNull(victim, "victim");
		return yieldSelf();
	}

	@Override
	public B operator(Operator operator) {
		this.operator = Objects.requireNonNull(operator, "operator");
		return yieldSelf();
	}

}
