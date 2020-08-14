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
 * A punishment ready to be created, which does not yet have an ID
 * 
 * @author A248
 *
 */
public final class DraftPunishment extends AbstractPunishment {
	
	private DraftPunishment(PunishmentType type, Victim victim, Operator operator, String reason, Scope scope, long start, long end) {
		super(type, victim, operator, reason, scope, start, end);
	}
	
	/**
	 * Builder of draft punishments. Should not be shared across threads
	 * 
	 * @author A248
	 *
	 */
	public static class Builder {
		
		private PunishmentType type;
		private Victim victim;
		private Operator operator;
		private String reason;
		private Scope scope;
		private Long start;
		private long end = 0L;
		
		/**
		 * Creates the builder
		 * 
		 */
		public Builder() {
			
		}
		
		/**
		 * Sets the type of this builder to the given one
		 * 
		 * @param type the punishment type
		 * @return this builder
		 * @throws NullPointerException if {@code type} is null
		 */
		public Builder type(PunishmentType type) {
			this.type = Objects.requireNonNull(type, "type");
			return this;
		}
		
		/**
		 * Sets the victim of this builder to the given one
		 * 
		 * @param victim the victim
		 * @return this builder
		 * @throws NullPointerException if {@code victim} is null
		 */
		public Builder victim(Victim victim) {
			this.victim = Objects.requireNonNull(victim, "victim");
			return this;
		}
		
		/**
		 * Sets the operator of this builder to the given one
		 * 
		 * @param operator the operator
		 * @return this builder
		 * @throws NullPointerException if {@code operator} is null
		 */
		public Builder operator(Operator operator) {
			this.operator = Objects.requireNonNull(operator, "operator");
			return this;
		}
		
		/**
		 * Sets the reason of this builder to the given one
		 * 
		 * @param reason the reason
		 * @return this builder
		 * @throws NullPointerException if {@code reason} is null
		 */
		public Builder reason(String reason) {
			this.reason = Objects.requireNonNull(reason, "reason");
			return this;
		}
		
		/**
		 * Sets the scope of this builder to the given one
		 * 
		 * @param scope the scope
		 * @return this builder
		 * @throws NullPointerException if {@code scope} is null
		 */
		public Builder scope(Scope scope) {
			this.scope = Objects.requireNonNull(scope, "scope");
			return this;
		}
		
		/**
		 * Sets the start time of this builder to the given one. The time is in unix seconds,
		 * NOT miliseconds. <br>
		 * <br>
		 * <b>To use the current time as the start time, don't call this method!</b>
		 * The API will fill in the current time automatically.
		 * 
		 * @param start the start time in unix seconds
		 * @return this builder
		 */
		public Builder start(long start) {
			if (start < 0L) {
				throw new IllegalArgumentException("Start time must be greater than or equal to 0");
			}
			this.start = start;
			return this;
		}
		
		/**
		 * Sets the end time of this builder to <i>permanent</i>. This is the default
		 * 
		 * @return this builder
		 */
		public Builder permanent() {
			return end(0L);
		}
		
		/**
		 * Sets the end time of this builder to the given one. The time is in unix seconds,
		 * NOT milliseconds. <br>
		 * <br>
		 * For a permanent punishment, don't call this method. The API will make the punishment
		 * permanent unless specified otherwise.
		 * 
		 * @param end the end time in unix seconds, or 0 for permanent
		 * @return this builder
		 */
		public Builder end(long end) {
			if (end < -1L) {
				throw new IllegalArgumentException("End time must be greater than or equal to 0, or -1 for permanent");
			}
			this.end = end;
			return this;
		}
		
		/**
		 * Builds into a {@link DraftPunishment}. <br>
		 * <br>
		 * Requires that this builder's type, victim, operator, reason, and scope are set. If any are unset,
		 * {@code IllegalArgumentException} is thrown
		 * 
		 * @return the draft punishment
		 * @throws IllegalArgumentException if any detail has not been set
		 */
		public DraftPunishment build() {
			if (type == null || victim == null || operator == null || reason == null || scope == null) {
				throw new IllegalArgumentException("Punishment details have not been set");
			}
			return new DraftPunishment(type, victim, operator, reason, scope,
					(start != null) ? start : System.currentTimeMillis() / 1_000L, end);
		}
		
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
		result = prime * result + (int) (start ^ (start >>> 32));
		result = prime * result + (int) (end ^ (end >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DraftPunishment)) {
			return false;
		}
		DraftPunishment that = (DraftPunishment) object;
		return type == that.type && victim.equals(that.victim) && operator.equals(that.operator)
				&& reason.equals(that.reason) && scope.equals(that.scope) && start == that.start && end == that.end;
	}
	
}
