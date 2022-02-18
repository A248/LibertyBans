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

package space.arim.libertybans.core.database.sql;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import space.arim.libertybans.core.config.Configs;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class AccountExpirationCondition {

	private final Field<Instant> updatedField;

	public AccountExpirationCondition(Field<Instant> updatedField) {
		this.updatedField = Objects.requireNonNull(updatedField, "updatedField");
	}

	public Condition isNotExpired(Configs configs, Instant currentTime) {
		var altAccountExpiration = configs.getMainConfig().enforcement().altAccountExpiration();
		if (!altAccountExpiration.enable()) {
			return DSL.noCondition();
		}
		Instant expiredBefore = currentTime.minus(Duration.ofDays(altAccountExpiration.expirationTimeDays()));
		return updatedField.greaterThan(expiredBefore);
	}

	@Override
	public String toString() {
		return "AccountExpirationCondition{" +
				"updatedField=" + updatedField +
				'}';
	}
}
