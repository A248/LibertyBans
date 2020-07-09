/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api;

import java.util.Objects;

/**
 * Abstract implementation of {@link PunishmentBase} so that {@link Punishment} implementations
 * and {@link DraftPunishment} itself may avoid boilerplate.
 * 
 * @author A248
 *
 */
public abstract class AbstractPunishment implements PunishmentBase {

	// Package visible so DraftPunishment may use them in equals/hashCode
	
	final PunishmentType type;
	final Victim victim;
	final Operator operator;
	final String reason;
	final Scope scope;
	final long start;
	final long end;
	
	protected AbstractPunishment(PunishmentType type, Victim victim, Operator operator, String reason, Scope scope, long start, long end) {
		this.type = Objects.requireNonNull(type, "PunishmentType must not be null");
		this.victim = Objects.requireNonNull(victim, "Victim must not be null");
		this.operator = Objects.requireNonNull(operator, "Operator must not be null");
		this.reason = Objects.requireNonNull(reason, "Reason must not be null");
		this.scope = Objects.requireNonNull(scope, "Scope must not be null");
		this.start = start;
		if (start < 0L) {
			throw new IllegalArgumentException("Start time must be greater than or equal to 0");
		}
		this.end = end;
		if (end < -1L) {
			throw new IllegalArgumentException("End time must be greater than or equal to 0, or -1 for permanent");
		}
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
	public Scope getScope() {
		return scope;
	}
	
	@Override
	public long getStart() {
		return start;
	}
	
	@Override
	public long getEnd() {
		return end;
	}

	@Override
	public String toString() {
		if (this instanceof Punishment) {
			return getClass().getSimpleName() + " [id=" + ((Punishment) this).getID() + ", type=" + type + ", victim="
					+ victim + ", operator=" + operator + ", reason=" + reason + ", scope=" + scope + ", start=" + start
					+ ", end=" + end + "]";
		} else {
			return getClass().getSimpleName() + " [type=" + type + ", victim=" + victim + ", operator=" + operator
					+ ", reason=" + reason + ", scope=" + scope + ", start=" + start + ", end=" + end + "]";
		}
	}

	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object object);
	
}
