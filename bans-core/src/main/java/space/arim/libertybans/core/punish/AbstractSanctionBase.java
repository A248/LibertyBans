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

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.SanctionBase;

import java.util.Objects;

abstract class AbstractSanctionBase implements SanctionBase {

	private final Victim victim;
	private final Operator operator;

	AbstractSanctionBase(Victim victim, Operator operator) {
		this.victim = Objects.requireNonNull(victim, "victim");
		this.operator = Objects.requireNonNull(operator, "operator");
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractSanctionBase that = (AbstractSanctionBase) o;
		return victim.equals(that.victim) && operator.equals(that.operator);
	}

	@Override
	public int hashCode() {
		int result = victim.hashCode();
		result = 31 * result + operator.hashCode();
		return result;
	}

	@Override
	public abstract String toString();

}
