/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Duration;
import java.time.Instant;

@Singleton
public class Enactor implements PunishmentDrafter {

	private final InternalScopeManager scopeManager;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	private final Time time;

	@Inject
	public Enactor(InternalScopeManager scopeManager, Provider<InternalDatabase> dbProvider,
				   PunishmentCreator creator, Time time) {
		this.scopeManager = scopeManager;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.time = time;
	}

	@Override
	public DraftPunishmentBuilder draftBuilder() {
		return new DraftPunishmentBuilderImpl(this);
	}

	InternalScopeManager scopeManager() {
		return scopeManager;
	}

	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		InternalDatabase database = dbProvider.get();

		final PunishmentType type = draftPunishment.getType();
		final Duration duration = draftPunishment.getDuration();
		final Instant start = time.currentTimestamp();
		final Instant end = (duration.isZero()) ?
				Punishment.PERMANENT_END_DATE : start.plusSeconds(duration.toSeconds());

		Enaction enaction = new Enaction(
				new Enaction.OrderDetails(
						type, draftPunishment.getVictim(), draftPunishment.getOperator(),
						draftPunishment.getReason(), draftPunishment.getScope(), start, end),
				creator);

		return database.queryWithRetry((context, transaction) -> {
			if (type != PunishmentType.KICK) {
				database.clearExpiredPunishments(context, type, start);
			}
			return enaction.enactActive(context, transaction);
		});
	}

}
