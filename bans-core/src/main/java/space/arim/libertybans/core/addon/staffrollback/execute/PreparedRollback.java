/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.addon.staffrollback.execute;

import space.arim.libertybans.api.Operator;

import java.time.Instant;
import java.util.Objects;

/**
 * A rollback ready to be executed. Used to implement confirmation
 */
public final class PreparedRollback {

	private final Operator operator;
	private final Instant minStartTime;
	private final Instant maxStartTime;

	public PreparedRollback(Operator operator, Instant minStartTime, Instant maxStartTime) {
		this.operator = Objects.requireNonNull(operator, "operator");
		this.minStartTime = Objects.requireNonNull(minStartTime, "minStartTime");
		this.maxStartTime = Objects.requireNonNull(maxStartTime, "maxStartTime");
	}

	public Operator operator() {
		return operator;
	}

	/**
	 * The minimum start time, inclusive
	 *
	 * @return the min start time after which to rollback
	 */
	public Instant minStartTime() {
		return minStartTime;
	}

	/**
	 * The maximum start time, inclusive
	 *
	 * @return the max start time after which to rollback
	 */
	public Instant maxStartTime() {
		return maxStartTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PreparedRollback that = (PreparedRollback) o;
		return operator.equals(that.operator) && minStartTime.equals(that.minStartTime)
				&& maxStartTime.equals(that.maxStartTime);
	}

	@Override
	public int hashCode() {
		int result = operator.hashCode();
		result = 31 * result + minStartTime.hashCode();
		result = 31 * result + maxStartTime.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "PreparedRollback{" +
				"operator=" + operator +
				", minStartTime=" + minStartTime +
				", maxStartTime=" + maxStartTime +
				'}';
	}
}
