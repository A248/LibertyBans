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

package space.arim.libertybans.core.database.jooq;

import org.jooq.DSLContext;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.scope.ScopeImpl;

import java.time.Instant;
import java.util.UUID;

import static space.arim.libertybans.core.schema.Tables.BANS;
import static space.arim.libertybans.core.schema.Tables.PUNISHMENTS;
import static space.arim.libertybans.core.schema.Tables.VICTIMS;

public final class JooqClassloading {

	private final JooqContext jooqContext;

	public JooqClassloading(JooqContext jooqContext) {
		this.jooqContext = jooqContext;
	}

	public void preinitializeClasses() {
		long startNanos = System.nanoTime();
		DSLContext context = jooqContext.createRenderOnlyContext();
		context
				.insertInto(VICTIMS)
				.columns(VICTIMS.ID, VICTIMS.TYPE, VICTIMS.UUID, VICTIMS.ADDRESS)
				.values(0, Victim.VictimType.PLAYER, new UUID(0, 0), NetworkAddress.of(new byte[4]))
				.getSQL();
		context
				.insertInto(PUNISHMENTS)
				.columns(
						PUNISHMENTS.ID, PUNISHMENTS.TYPE,
						PUNISHMENTS.OPERATOR, PUNISHMENTS.REASON,
						PUNISHMENTS.SCOPE, PUNISHMENTS.START, PUNISHMENTS.END)
				.values(
						0L, PunishmentType.BAN,
						ConsoleOperator.INSTANCE, "",
						ScopeImpl.GLOBAL, Instant.EPOCH, Instant.MAX
				)
				.getSQL();
		context
				.insertInto(BANS)
				.columns(BANS.ID, BANS.VICTIM)
				.values(0L, 0)
				.getSQL();
		long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
		if (elapsedMillis >= 250) {
			String elapsedSeconds = String.format("%.2f", ((double) elapsedMillis) / 1000D);
			LoggerFactory.getLogger(getClass())
					.info("Ensured JOOQ classes were loaded in {} seconds", elapsedSeconds);
		}
	}
}
