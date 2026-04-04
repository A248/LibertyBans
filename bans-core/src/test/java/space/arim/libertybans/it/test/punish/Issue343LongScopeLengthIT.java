/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.it.test.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.core.database.sql.ScopeIdSequenceValue;
import space.arim.libertybans.core.database.sql.SequenceValue;
import space.arim.libertybans.core.database.sql.VictimIdSequenceValue;
import space.arim.libertybans.core.scope.ScopeType;
import space.arim.libertybans.core.scope.SpecificServerScope;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.ThrowawayInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.jooq.impl.DSL.castNull;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.val;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_PUNISHMENT_IDS;
import static space.arim.libertybans.core.schema.Tables.*;
import static space.arim.libertybans.core.schema.tables.History.HISTORY;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;
import static space.arim.libertybans.core.schema.tables.Scopes.SCOPES;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;
import static space.arim.libertybans.it.util.TestingUtil.unwrapInnerEx;

@ExtendWith(InjectionInvocationContextProvider.class)
public class Issue343LongScopeLengthIT {

    private static final Victim sampleVictim = PlayerVictim.of(UUID.randomUUID());
    private static final String LONG_SCOPE = UUID.randomUUID().toString();

    @BeforeAll
    public static void checkLong() {
        assert LONG_SCOPE.length() > 32;
    }

    void insertSamplePunishment(PunishmentDrafter drafter, ScopeManager scopeManager) {
        drafter
                .draftBuilder()
                .type(PunishmentType.BAN)
                .victim(sampleVictim)
                .operator(ConsoleOperator.INSTANCE)
                .reason("because I said so")
                .scope(scopeManager.specificScope(LONG_SCOPE))
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join();
    }

    @TestTemplate
    public void throwBeforeInsert(PunishmentDrafter drafter, ScopeManager scopeManager) {
        assertThrows(IllegalArgumentException.class, unwrapInnerEx(() -> insertSamplePunishment(drafter, scopeManager)));
    }

    @ExtendWith(InjectionInvocationContextProvider.class)
    @ThrowawayInstance
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    public class ForwardCompatibility {

        private final Provider<InternalDatabase> dbProvider;

        @Inject
        public ForwardCompatibility(Provider<InternalDatabase> dbProvider) {
            this.dbProvider = dbProvider;
        }

        @BeforeEach
        public void adjustScopeTableLimit() {
            InternalDatabase db = dbProvider.get();
            db.execute(context -> {
                context.dropView(APPLICABLE_ACTIVE).execute();
                context.dropView(APPLICABLE_HISTORY).execute();
                context.dropView(APPLICABLE_BANS).execute();
                context.dropView(APPLICABLE_MUTES).execute();
                context.dropView(APPLICABLE_WARNS).execute();
                context.dropView(SIMPLE_ACTIVE).execute();
                context.dropView(SIMPLE_HISTORY).execute();
                context.dropView(SIMPLE_BANS).execute();
                context.dropView(SIMPLE_MUTES).execute();
                context.dropView(SIMPLE_WARNS).execute();

                context
                        .alterTable(SCOPES)
                        .alter(SCOPES.VALUE)
                        .set(SQLDataType.VARCHAR(255))
                        .execute();
                /*
                Refresh views

                Re-run the view creation statements in the database migration. Read the resource file and split by ';'
                to obtain SQL statements and execute them. Replace placeholders like in Flyway migrations. However,
                skip the first two queries which do not relate to views.
                 */
                StringBuilder statement = new StringBuilder();
                URL refreshViewsUrl = getClass().getResource("/database-migrations/V36__Server_scopes.sql");
                assert refreshViewsUrl != null;
                Vendor vendor = db.getVendor();
                Map<String, String> placeholders = Map.of(
                        "${tableprefix}", "libertybans_",
                        "${extratableoptions}", vendor.getExtraTableOptions(),
                        "${alterviewstatement}", "CREATE VIEW",
                        "${zerosmallintliteral}", vendor.zeroSmallintLiteral(),
                        "${migratescopestart}", vendor.migrateScopePostIssue343()[0],
                        "${migratescopeend}", vendor.migrateScopePostIssue343()[1]
                );
                int skipCount = 2;
                try (InputStream stream = refreshViewsUrl.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("--")) {
                            continue;
                        }
                        statement.append(' ').append(line);
                        if (line.endsWith(";")) {
                            String statementSql = statement.toString();
                            statement = new StringBuilder();
                            if (skipCount > 0) {
                                skipCount--;
                                continue;
                            }
                            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                                statementSql = statementSql.replace(placeholder.getKey(), placeholder.getValue());
                            }
                            context.query(statementSql).execute();
                        }
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }).join();
        }

        @TestTemplate
        public void stillThrowBeforeInsert(PunishmentDrafter drafter, ScopeManager scopeManager) {
            assertThrows(IllegalArgumentException.class, unwrapInnerEx(() -> insertSamplePunishment(drafter, scopeManager)));
        }

        @TestTemplate
        public void acceptFromDatabase(PunishmentSelector selector) {
            // Setup: simulate a punishment inserted by 1.2.0 with a longer scope length
            dbProvider.get().execute(context -> {
                Integer scopeId = context.select(
                        new ScopeIdSequenceValue(context).nextValue()
                ).fetchSingle().value1();
                context
                        .insertInto(SCOPES)
                        .columns(SCOPES.ID, SCOPES.TYPE, SCOPES.VALUE)
                        .values(val(scopeId), inline(ScopeType.SERVER), val(LONG_SCOPE))
                        .execute();

                SequenceValue<Long> punishmentIdSequence = new SequenceValue<>(context, LIBERTYBANS_PUNISHMENT_IDS);
                context
                        .insertInto(PUNISHMENTS)
                        .columns(
                                PUNISHMENTS.ID, PUNISHMENTS.TYPE, PUNISHMENTS.OPERATOR, PUNISHMENTS.REASON,
                                PUNISHMENTS.SCOPE, PUNISHMENTS.START, PUNISHMENTS.END,
                                PUNISHMENTS.TRACK, PUNISHMENTS.SCOPE_ID
                        )
                        .values(
                                punishmentIdSequence.nextValue(), val(PunishmentType.BAN, PUNISHMENTS.TYPE),
                                val(ConsoleOperator.INSTANCE, PUNISHMENTS.OPERATOR),
                                val("some reason", PUNISHMENTS.REASON), val("", PUNISHMENTS.SCOPE),
                                val(Instant.now(), PUNISHMENTS.START), val(Punishment.PERMANENT_END_DATE, PUNISHMENTS.END),
                                castNull(Integer.class), val(scopeId)
                        )
                        .execute();

                Integer victimId = context.select(
                        new VictimIdSequenceValue(context).retrieveVictimId(sampleVictim)
                ).fetchSingle().value1();

                context
                        .insertInto(HISTORY)
                        .columns(HISTORY.ID, HISTORY.VICTIM)
                        .values(punishmentIdSequence.lastValueInSession(), val(victimId))
                        .execute();
            }).join();

            // debugging
            dbProvider.get().execute(context -> {
                context
                        .select(SCOPES.ID, SCOPES.VALUE)
                        .from(SCOPES)
                        .fetch();

                context
                        .select(PUNISHMENTS.ID, PUNISHMENTS.SCOPE)
                        .from(PUNISHMENTS)
                        .fetch();

                context
                        .select(SIMPLE_HISTORY.ID, SIMPLE_HISTORY.SCOPE)
                        .from(SIMPLE_HISTORY)
                        .fetch();
            }).join();
            List<Punishment> history = selector
                    .selectionBuilder()
                    .victim(sampleVictim)
                    .selectAll()
                    .build()
                    .getAllSpecificPunishments()
                    .toCompletableFuture()
                    .join();
            assertEquals(1, history.size(), () -> "Received history of " + history);
            Punishment singlePunishment = history.get(0);
            assertEquals(sampleVictim, singlePunishment.getVictim());
            ServerScope scope = singlePunishment.getScope();
            assertEquals(LONG_SCOPE, ((SpecificServerScope) scope).server());
        }
    }
}
