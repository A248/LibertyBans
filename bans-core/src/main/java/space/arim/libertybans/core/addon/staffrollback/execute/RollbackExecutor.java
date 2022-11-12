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

package space.arim.libertybans.core.addon.staffrollback.execute;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;

public final class RollbackExecutor {

	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentSelector selector;

	@Inject
	public RollbackExecutor(Provider<InternalDatabase> dbProvider, PunishmentSelector selector) {
		this.dbProvider = dbProvider;
		this.selector = selector;
	}

	public CentralisedFuture<Integer> previewCount(PreparedRollback rollback) {
		return selector.selectionBuilder()
				.operator(rollback.operator())
				.seekAfter(rollback.minStartTime(), 0L)
				.seekBefore(rollback.maxStartTime(), Long.MAX_VALUE)
				.selectAll()
				.build()
				.countNumberOfPunishments()
				.toCompletableFuture();
	}

	public CentralisedFuture<Integer> executeRollback(PreparedRollback rollback) {
		return dbProvider.get().queryWithRetry((context, transaction) -> {
			return context
					.deleteFrom(PUNISHMENTS)
					.where(PUNISHMENTS.OPERATOR.eq(rollback.operator()))
					.and(PUNISHMENTS.START.between(rollback.minStartTime(), rollback.maxStartTime()))
					.execute();
		});
	}
}
