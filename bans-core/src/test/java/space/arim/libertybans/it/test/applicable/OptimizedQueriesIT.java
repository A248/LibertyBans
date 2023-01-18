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

package space.arim.libertybans.it.test.applicable;

import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.jooq.JooqContext;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.selector.SelectionByApplicabilityImpl;
import space.arim.libertybans.core.selector.SelectorImpl;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver.NotAKick;
import space.arim.libertybans.it.util.RandomUtil;

import java.time.Instant;
import java.util.UUID;

import static org.jooq.impl.DSL.inline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.core.schema.tables.StrictLinks.STRICT_LINKS;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(RandomPunishmentTypeResolver.class)
public class OptimizedQueriesIT {

	private final SelectorImpl selector;

	@Inject
	public OptimizedQueriesIT(SelectorImpl selector) {
		this.selector = selector;
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void handwrittenQueriesAreIdentical(InternalDatabase database, @DontInject @NotAKick PunishmentType type) {
		DSLContext context = new JooqContext(database.getVendor().dialect()).createRenderOnlyContext();

		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();

		SelectionByApplicabilityImpl selection = selector
				.selectionByApplicabilityBuilder(uuid, address)
				.type(type)
				.build();
		assertEquals(
				renderHandwrittenQuery(context, uuid, address, selection.getAddressStrictness(), type),
				selection.renderSingleApplicablePunishmentSQL(context)
		);
	}

	private String renderHandwrittenQuery(DSLContext context, UUID uuid, NetworkAddress address,
										  AddressStrictness strictness, PunishmentType type) {
		var simpleView = new TableForType(type).simpleView();
		var applView = new TableForType(type).applicableView();

		Select<?> select = switch (strictness) {
			case LENIENT -> context
					.select(
							simpleView.id(),
							simpleView.victimType(), simpleView.victimUuid(), simpleView.victimAddress(),
							simpleView.operator(), simpleView.reason(),
							simpleView.scope(), simpleView.start(), simpleView.end()
					)
					.from(simpleView.table())
					.where(new VictimCondition(simpleView).simplyMatches(uuid, address))
					.and(new EndTimeCondition(simpleView).isNotExpired(Instant.EPOCH))
					.orderBy(new EndTimeOrdering(simpleView).expiresLeastSoon())
					.limit(inline(1));
			case NORMAL -> context
					.select(
							applView.id(),
							applView.victimType(), applView.victimUuid(), applView.victimAddress(),
							applView.operator(), applView.reason(),
							applView.scope(), applView.start(), applView.end()
					).from(applView.table())
					.where(applView.uuid().eq(uuid))
					.and(new EndTimeCondition(applView).isNotExpired(Instant.EPOCH))
					.orderBy(new EndTimeOrdering(applView).expiresLeastSoon())
					.limit(inline(1));
			case STERN, STRICT -> context
					.select(
							applView.id(),
							applView.victimType(), applView.victimUuid(), applView.victimAddress(),
							applView.operator(), applView.reason(),
							applView.scope(), applView.start(), applView.end()
					).from(applView.table())
					.innerJoin(STRICT_LINKS)
					.on(applView.uuid().eq(STRICT_LINKS.UUID1))
					.where((strictness == AddressStrictness.STERN) ?
							// STERN
							// appl.uuid = strict_links.uuid1 = uuid
							// OR victim_type != 'PLAYER' AND strict_links.uuid2 = uuid
							STRICT_LINKS.UUID1.eq(uuid).or(
									STRICT_LINKS.UUID2.eq(uuid).and(applView.victimType().notEqual(inline(VictimType.PLAYER))))
							// STRICT
							// strict_links.uuid2 = uuid
							: STRICT_LINKS.UUID2.eq(uuid)
					)
					.and(new EndTimeCondition(applView).isNotExpired(Instant.EPOCH))
					.orderBy(new EndTimeOrdering(applView).expiresLeastSoon())
					.limit(inline(1));
		};
		return select.getSQL();
	}

	record EndTimeOrdering(Field<Instant> endField) {

		EndTimeOrdering(PunishmentFields fields) {
			this(fields.end());
		}

		OrderField<?> expiresLeastSoon() {
			var end = endField.coerce(Long.class);
			return DSL.choose(end)
					.when(inline(0L), inline(Long.MAX_VALUE))
					.otherwise(end)
					.desc();
		}

	}

}
