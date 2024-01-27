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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.CalculablePunishment;
import space.arim.libertybans.api.punish.CalculablePunishmentBuilder;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.execute.SQLRunnable;
import space.arim.libertybans.core.database.execute.SQLTransactionalFunction;
import space.arim.libertybans.core.database.execute.SQLTransactionalRunnable;
import space.arim.libertybans.core.database.execute.Transaction;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.selector.SelectionResources;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

@Singleton
public class Enactor implements PunishmentDrafter {

	private final FactoryOfTheFuture futuresFactory;
	private final InternalScopeManager scopeManager;
	private final Provider<InternalDatabase> dbProvider;
	private final InternalSelector selector;
	private final PunishmentCreator creator;
	private final Time time;

	@Inject
	public Enactor(FactoryOfTheFuture futuresFactory, InternalScopeManager scopeManager, Provider<InternalDatabase> dbProvider,
				   InternalSelector selector, PunishmentCreator creator, Time time) {
		this.futuresFactory = futuresFactory;
		this.scopeManager = scopeManager;
		this.dbProvider = dbProvider;
		this.selector = selector;
		this.creator = creator;
		this.time = time;
	}

	@Override
	public DraftPunishmentBuilder draftBuilder() {
		return new DraftPunishmentBuilderImpl(this);
	}

	@Override
	public CalculablePunishmentBuilder calculablePunishmentBuilder() {
		return new CalculablePunishmentBuilderImpl(this);
	}

	InternalScopeManager scopeManager() {
		return scopeManager;
	}

	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		InternalDatabase database = dbProvider.get();

		final PunishmentType type = draftPunishment.getType();
		final Duration duration = draftPunishment.getDuration();
		final Instant start = time.currentTimestamp();
		final Instant end = duration.isZero() ?
				Punishment.PERMANENT_END_DATE : start.plusSeconds(duration.toSeconds());

		Enaction enaction = new Enaction(
				new Enaction.OrderDetails(
						type, draftPunishment.getVictim(), draftPunishment.getOperator(),
						draftPunishment.getReason(), draftPunishment.getScope(),
						start, end, draftPunishment.getEscalationTrack().orElse(null)
				),
				creator);

		return database.queryWithRetry((context, transaction) -> {
			// Make sure concurrent executions do not conflict
			transaction.setIsolation(Connection.TRANSACTION_SERIALIZABLE);

			if (type != PunishmentType.KICK) {
				database.clearExpiredPunishments(context, type, start);
			}
			// If we rollback punishment enactment due to a conflicting ban or mute,
			// it is not necessary to undo clearing expired punishments
			return transaction.executeNested(enaction::enactActive);
		});
	}

	CentralisedFuture<Punishment> calculatePunishment(CalculablePunishment calculablePunishment) {

		final Victim victim = calculablePunishment.getVictim();
		final Operator operator = calculablePunishment.getOperator();
		final Instant start = time.currentTimestamp();
		final EscalationTrack escalationTrack = calculablePunishment.getEscalationTrack();

		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			// Make sure concurrent executions do not conflict
			transaction.setIsolation(Connection.TRANSACTION_SERIALIZABLE);

			var calculationResult = calculablePunishment.getCalculator().compute(
					escalationTrack, victim,
					selector.selectionBuilder(selectionResourcesUsing(context, transaction))
			);
			PunishmentType type = calculationResult.type();
			if (type != PunishmentType.KICK) {
				database.clearExpiredPunishments(context, type, start);
			}
			Duration duration = calculationResult.duration();
			ServerScope scope = scopeManager.checkScope(calculationResult.scope());
			Instant end = duration.isZero() ?
					Punishment.PERMANENT_END_DATE : start.plusSeconds(duration.toSeconds());

			Enaction enaction = new Enaction(
					new Enaction.OrderDetails(
							type, victim, operator,
							calculationResult.reason(), scope,
							start, end, escalationTrack
					),
					creator);
			// Again, a rollback here does not mandate undoing work from above
			return transaction.executeNested(enaction::enactActive);
		});
	}

	private SelectionResources selectionResourcesUsing(DSLContext context, Transaction transaction) {
		class ContextualExecutor implements QueryExecutor {

			private <E extends Throwable> E rollbackBeforeThrow(E reason) throws E {
				try {
					transaction.rollback();
				} catch (RuntimeException suppressed) {
					reason.addSuppressed(suppressed);
				}
				throw reason;
			}

			@Override
			public void executeWithExistingConnection(Connection connection, SQLTransactionalRunnable command) throws SQLException {
				throw new UnsupportedOperationException();
			}

			@Override
			public CentralisedFuture<Void> execute(SQLRunnable command) {
				try {
					command.run(context);
				} catch (RuntimeException ex) {
					throw rollbackBeforeThrow(ex);
				}
				return futuresFactory.completedFuture(null);
			}

			@Override
			public <R> CentralisedFuture<R> query(SQLFunction<R> command) {
				R result;
				try {
					result = command.obtain(context);
				} catch (RuntimeException ex) {
					throw rollbackBeforeThrow(ex);
				}
				return futuresFactory.completedFuture(result);
			}

			@Override
			public CentralisedFuture<Void> executeWithRetry(int retryCount, SQLTransactionalRunnable command) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <R> CentralisedFuture<R> queryWithRetry(int retryCount, SQLTransactionalFunction<R> command) {
				throw new UnsupportedOperationException();
			}
		}
		QueryExecutor contextualExecutor = new ContextualExecutor();
		return new SelectionResources(
				futuresFactory,
				() -> contextualExecutor,
				scopeManager,
				creator,
				time
		);
	}
}
