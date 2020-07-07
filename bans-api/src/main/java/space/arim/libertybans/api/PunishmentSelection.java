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

/**
 * A selection which will match punishments in the database with certain details. <br>
 * To retrieve an instance, use the {@link Builder}
 * 
 * @author A248
 *
 */
public final class PunishmentSelection {

	private final PunishmentType type;
	private final Victim victim;
	private final Operator operator;
	private final Scope scope;
	private final boolean selectActiveOnly;
	
	PunishmentSelection(PunishmentType type, Victim victim, Operator operator, Scope scope, boolean selectActiveOnly) {
		this.type = type;
		this.victim = victim;
		this.operator = operator;
		this.scope = scope;
		this.selectActiveOnly = selectActiveOnly;
	}
	
	/**
	 * Gets the punishment type matched, or {@code null} to match all types
	 * 
	 * @return the punishment type or {@code null} for all types
	 */
	public PunishmentType getType() {
		return type;
	}
	
	/**
	 * Gets the victim matched, or {@code null} to match all victims
	 * 
	 * @return the victim or {@code null} for all victims
	 */
	public Victim getVictim() {
		return victim;
	}
	
	/**
	 * Gets the operator matched, or {@code null} to match all operators
	 * 
	 * @return the operator or {@code null} for all operators
	 */
	public Operator getOperator() {
		return operator;
	}
	
	/**
	 * Gets the scope matched, or {@code null} to match all scopes
	 * 
	 * @return the scope or {@code null} for all scopes
	 */
	public Scope getScope() {
		return scope;
	}
	
	/**
	 * Whether this selection will match only active, non-expired punishments. If {@code false},
	 * then this selection may match undone or expired punishments as drawn from the punishments
	 * history.
	 * 
	 * @return true to select only active and non-expired punishments, false to select all punishments
	 */
	public boolean selectActiveOnly() {
		return selectActiveOnly;
	}
	
	/**
	 * Builder of {@link PunishmentSelection}s
	 * 
	 * @author A248
	 *
	 */
	public static class Builder {
		
		private PunishmentType type;
		private Victim victim;
		private Operator operator;
		private Scope scope;
		private boolean selectActiveOnly = true;
		
		/**
		 * Sets the punishment type matched, or {@code null} to match all types
		 * 
		 * @param type the punishment type or {@code null} for all types
		 * @return this builder
		 */
		public Builder type(PunishmentType type) {
			this.type = type;
			return this;
		}
		
		/**
		 * Sets the victim matched, or {@code null} to match all victims
		 * 
		 * @param victim the victim or {@code null} for all victims
		 * @return this builder
		 */
		public Builder victim(Victim victim) {
			this.victim = victim;
			return this;
		}
		
		/**
		 * Sets the operator matched, or {@code null} to match all operators
		 * 
		 * @param operator the operator or {@code null} for all operators
		 * @return this builder
		 */
		public Builder operator(Operator operator) {
			this.operator = operator;
			return this;
		}
		
		/**
		 * Sets the scope matched, or {@code null} to match all scopes
		 * 
		 * @param scope the scope or {@code null} for all scopes
		 * @return this builder
		 */
		public Builder scope(Scope scope) {
			this.scope = scope;
			return this;
		}
		
		/**
		 * Sets to match only active, non-expired punishments.
		 * 
		 * @return this builder
		 */
		public Builder selectActiveOnly() {
			selectActiveOnly = true;
			return this;
		}
		
		/**
		 * Sets to match all punishments, including those undone or expired
		 * 
		 * @return this builder
		 */
		public Builder selectAll() {
			selectActiveOnly = false;
			return this;
		}
		
		/**
		 * Builds into a full {@link PunishmentSelection}
		 * 
		 * @return a selection based on this builder's details
		 */
		public PunishmentSelection build() {
			return new PunishmentSelection(type, victim, operator, scope, selectActiveOnly);
		}
		
	}
	
}
