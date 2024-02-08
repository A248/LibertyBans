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
package space.arim.libertybans.core.event;

import java.util.Objects;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.event.PardonEvent;

public class PardonEventImpl extends AbstractCancellable implements PardonEvent {

	private final Operator operator;
	private final Victim victim;
	private final PunishmentType type;
	private final String reason;

	public PardonEventImpl(Operator operator, Victim victim, PunishmentType type, String reason) {
		this.operator = Objects.requireNonNull(operator, "operator");
		this.victim = Objects.requireNonNull(victim, "victim");
		this.type = Objects.requireNonNull(type, "type");
		this.reason = Objects.requireNonNull(reason, "reason");
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public Victim getPardonedVictim() {
		return victim;
	}

	@Override
	public PunishmentType getPunishmentType() {
		return type;
	}

	@Override
	public String getReason() {
		return reason;
	}
}
