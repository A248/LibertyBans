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

public final class PunishmentSelection {

	private final PunishmentType type;
	private final Victim victim;
	private final Operator operator;
	private final Scope scope;
	
	public PunishmentSelection(PunishmentType type, Victim victim, Operator operator, Scope scope) {
		this.type = type;
		this.victim = victim;
		this.operator = operator;
		this.scope = scope;
	}
	
	public PunishmentType getType() {
		return type;
	}
	
	public Victim getVictim() {
		return victim;
	}
	
	public Operator getOperator() {
		return operator;
	}
	
	public Scope getScope() {
		return scope;
	}
	
	public static class Builder {
		
		private PunishmentType type;
		private Victim victim;
		private Operator operator;
		private Scope scope;
		
		public Builder type(PunishmentType type) {
			this.type = type;
			return this;
		}
		
		public Builder victim(Victim victim) {
			this.victim = victim;
			return this;
		}
		
		public Builder operator(Operator operator) {
			this.operator = operator;
			return this;
		}
		
		public Builder scope(Scope scope) {
			this.scope = scope;
			return this;
		}
		
		public PunishmentSelection build() {
			return new PunishmentSelection(type, victim, operator, scope);
		}
		
	}
	
}
