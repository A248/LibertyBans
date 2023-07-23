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

package space.arim.libertybans.core.event;

import java.util.Objects;
import java.util.Optional;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.event.PostPardonEvent;
import space.arim.libertybans.api.punish.Punishment;

public record PostPardonEventImpl(Operator operator, Punishment punishment, String target) implements PostPardonEvent {

	public PostPardonEventImpl {
		Objects.requireNonNull(operator);
		Objects.requireNonNull(punishment);
		Objects.requireNonNull(target);
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public Punishment getPunishment() {
		return punishment;
	}

	@Override
	public Optional<String> getTarget() {
		return Optional.of(target);
	}

}
