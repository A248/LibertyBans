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
import org.jooq.Field;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentEditor;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.jooq.impl.DSL.greatest;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.when;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;

public final class Modifier {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final InternalScopeManager scopeManager;
	private final PunishmentCreator creator;
	private final GlobalEnforcement enforcement;

	@Inject
	public Modifier(FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
					InternalScopeManager scopeManager, PunishmentCreator creator, GlobalEnforcement enforcement) {
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.scopeManager = scopeManager;
		this.creator = creator;
		this.enforcement = enforcement;
	}

	class Editor implements PunishmentEditor {

		private final Punishment oldInstance;
		private String reason;
		private ServerScope scope;
		private Instant endDate;
		private Duration endDateDelta;

		Editor(Punishment oldInstance) {
			this.oldInstance = oldInstance;
		}

		@Override
		public void setReason(String reason) {
			this.reason = Objects.requireNonNull(reason);
		}

		@Override
		public void setScope(ServerScope scope) {
			scopeManager.checkScope(scope);
			this.scope = scope;
		}

		@Override
		public void setEndDate(Instant endDate) {
			if (endDateDelta != null) {
				throw new IllegalStateException("Cannot use setEndDate and extendEndDate simultaneously");
			}
			this.endDate = Objects.requireNonNull(endDate);
		}

		@Override
		public void extendEndDate(Duration endDateDelta) {
			if (endDate != null) {
				throw new IllegalStateException("Cannot use setEndDate and extendEndDate simultaneously");
			}
			this.endDateDelta = Objects.requireNonNull(endDateDelta);
		}

		ReactionStage<Optional<Punishment>> modify() {
			if (reason == null && scope == null && endDate == null && endDateDelta == null) {
				return futuresFactory.completedFuture(Optional.of(oldInstance));
			}
			return dbProvider.get().queryWithRetry((context, transaction) -> {
				var record = context.newRecord(PUNISHMENTS);
				if (reason != null) {
					record.setReason(reason);
				}
				if (scope != null) {
					record.setScope(scope);
				}
				if (endDate != null) {
					record.setEnd(endDate);
				}
				Map<Field<?>, Field<?>> endDateExtension = Map.of();
				if (endDateDelta != null) {
					Field<Instant> newEndDate = when(
							// Pass-through permanent punishments
							PUNISHMENTS.END.eq(inline(Punishment.PERMANENT_END_DATE)), inline(Punishment.PERMANENT_END_DATE, PUNISHMENTS.END)
					).otherwise(
							// For temporary punishments, guarantee positive duration
							greatest(PUNISHMENTS.START.plus(inline(1L)), PUNISHMENTS.END.plus(endDateDelta.getSeconds()))
					);
					endDateExtension = Map.of(PUNISHMENTS.END, newEndDate);
				}
				long id = oldInstance.getIdentifier();
				context
						.update(PUNISHMENTS)
						.set(record)
						.set(endDateExtension)
						.where(PUNISHMENTS.ID.eq(id))
						.execute();
				return context
						.select(PUNISHMENTS.REASON, PUNISHMENTS.SCOPE, PUNISHMENTS.END)
						.from(PUNISHMENTS)
						.where(PUNISHMENTS.ID.eq(id))
						// Can return null if punishment was expunged
						.fetchOne(creator.punishmentMapperForModifications(oldInstance));

			}).thenCompose((newInstance) -> {
				if (newInstance == null) {
					return futuresFactory.completedFuture(null);
				}
				return enforcement.updateDetails(newInstance).thenApply((ignore) -> newInstance);
			}).handle((updated, ex) -> {
				if (ex != null) {
					// Log and re-throw in case the caller does not use #completion
					LoggerFactory.getLogger(getClass()).warn(
							"Exception while updating punishment using editor {}", this, ex
					);
					throw ((ex instanceof RuntimeException rex) ? rex : new CompletionException(ex));
				}
				return Optional.ofNullable(updated);
			});
		}

		@Override
		public String toString() {
			return "Editor{" +
					"oldInstance=" + oldInstance +
					", reason='" + reason + '\'' +
					", scope=" + scope +
					", endDate=" + endDate +
					", endDateDelta=" + endDateDelta +
					'}';
		}

	}

}
