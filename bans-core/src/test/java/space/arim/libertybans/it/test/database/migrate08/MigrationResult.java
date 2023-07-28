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

package space.arim.libertybans.it.test.database.migrate08;

import org.jooq.DSLContext;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.importing.NameAddressRecord;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.scope.ScopeImpl;
import space.arim.libertybans.core.service.Time;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

record MigrationResult(List<Punishment> activePunishments,
					   List<Punishment> historicalPunishments) {

	MigrationResult {
		activePunishments = List.copyOf(activePunishments);
		historicalPunishments = List.copyOf(historicalPunishments);
	}

	static MigrationResult retrieveFrom(DSLContext context, PunishmentCreator creator) {
		return new MigrationResult(
				context
						.selectFrom(SIMPLE_ACTIVE)
						.orderBy(SIMPLE_ACTIVE.START)
						.fetch(creator.punishmentMapper()),
				context
						.selectFrom(SIMPLE_HISTORY)
						.orderBy(SIMPLE_HISTORY.START)
						.fetch(creator.punishmentMapper())
		);
	}

	static final class Builder {

		private final Time time;
		private final ZeroeightInterlocutor zeroeightInterlocutor;
		private final PunishmentCreator creator;

		private final List<Punishment> activePunishments = new ArrayList<>();
		private final List<Punishment> historicalPunishments = new ArrayList<>();

		private final AtomicInteger idGenerator = new AtomicInteger();

		Builder(Time time, ZeroeightInterlocutor zeroeightInterlocutor, PunishmentCreator creator) {
			this.time = Objects.requireNonNull(time, "time");
			this.zeroeightInterlocutor = Objects.requireNonNull(zeroeightInterlocutor, "zeroeightInterlocutor");
			this.creator = Objects.requireNonNull(creator, "creator");
		}

		Builder addUser(UUID uuid, String name, NetworkAddress address) {
			Instant currentTime = time.currentTimestamp();
			NameAddressRecord nameAddressRecord = new NameAddressRecord(uuid, name, address, currentTime);
			zeroeightInterlocutor.insertUser(nameAddressRecord);
			return this;
		}

		void addActivePunishment(PunishmentType type, Victim victim,
								 Operator operator, String reason, Duration duration) {
			addPunishment(type, victim, operator, reason, duration, true);
		}

		void addHistoricalPunishment(PunishmentType type, Victim victim,
									 Operator operator, String reason, Duration duration) {
			addPunishment(type, victim, operator, reason, duration, false);
		}

		private void addPunishment(PunishmentType type, Victim victim,
								   Operator operator, String reason, Duration duration,
								   boolean active) {
			Instant start = time.currentTimestamp();
			Instant end;
			if (duration.equals(Duration.ZERO)) { // Permanent
				end = Instant.MAX;
			} else {
				end = start.plus(duration);
			}
			long id = idGenerator.getAndIncrement();
			Punishment punishment = creator.createPunishment(
					id, type, victim, operator, reason, ScopeImpl.GLOBAL, start, end, null
			);
			zeroeightInterlocutor.insertPunishment(punishment, active);
			if (active) {
				activePunishments.add(punishment);
			}
			historicalPunishments.add(punishment);

		}

		MigrationResult build() {
			return new MigrationResult(activePunishments, historicalPunishments);
		}
	}
}
