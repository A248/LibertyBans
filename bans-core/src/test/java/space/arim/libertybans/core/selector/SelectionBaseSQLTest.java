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

package space.arim.libertybans.core.selector;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.jooq.JooqContext;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SelectionBaseSQLTest {

	@ParameterizedTest
	@EnumSource(AddressStrictness.class)
	public void optimizedApplicabilityQuery(AddressStrictness strictness) {
		SelectionResources selectionResources = new SelectionResources(
				new IndifferentFactoryOfTheFuture(), () -> mock(QueryExecutor.class),
				mock(InternalScopeManager.class), mock(PunishmentCreator.class), mock(Time.class)
		);
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = NetworkAddress.of(InetAddress.getLoopbackAddress());
		DSLContext context = new JooqContext(SQLDialect.HSQLDB).createRenderOnlyContext();

		String sql = new SelectionByApplicabilityBuilderImpl(selectionResources, uuid, address, strictness)
				.type(PunishmentType.BAN)
				.build()
				.renderSingleApplicablePunishmentSQL(context);
		assertEquals(
				expectedSql(strictness),
				sql
		);
	}

	private String expectedSql(AddressStrictness strictness) {
		return switch (strictness) {
			case LENIENT -> """
select "libertybans_simple_bans"."victim_type", "libertybans_simple_bans"."victim_uuid", \
"libertybans_simple_bans"."victim_address", "libertybans_simple_bans"."operator", \
"libertybans_simple_bans"."reason", "libertybans_simple_bans"."scope_type", "libertybans_simple_bans"."scope", \
"libertybans_simple_bans"."start", "libertybans_simple_bans"."end", "libertybans_simple_bans"."track", \
"libertybans_simple_bans"."id" \
from "libertybans_simple_bans" where \
(("libertybans_simple_bans"."end" = 0 or "libertybans_simple_bans"."end" > cast(? as bigint)) \
and \
(("libertybans_simple_bans"."victim_type" = 0 and "libertybans_simple_bans"."victim_uuid" = cast(? as uuid)) or \
("libertybans_simple_bans"."victim_type" = 1 and "libertybans_simple_bans"."victim_address" = cast(? as varbinary(16))) \
or ("libertybans_simple_bans"."victim_type" = 2 and ("libertybans_simple_bans"."victim_uuid" = cast(? as uuid) or \
"libertybans_simple_bans"."victim_address" = cast(? as varbinary(16)))))) \
order by case "libertybans_simple_bans"."end" \
when 0 then 9223372036854775807 else "libertybans_simple_bans"."end" end desc limit 1""";
			case NORMAL -> """
select "libertybans_applicable_bans"."victim_type", \
"libertybans_applicable_bans"."victim_uuid", "libertybans_applicable_bans"."victim_address", \
"libertybans_applicable_bans"."operator", "libertybans_applicable_bans"."reason", \
"libertybans_applicable_bans"."scope_type", "libertybans_applicable_bans"."scope", "libertybans_applicable_bans"."start", \
"libertybans_applicable_bans"."end", "libertybans_applicable_bans"."track", "libertybans_applicable_bans"."id" \
from "libertybans_applicable_bans" where \
(("libertybans_applicable_bans"."end" = 0 or "libertybans_applicable_bans"."end" > cast(? as bigint)) \
and \
"libertybans_applicable_bans"."uuid" = cast(? as uuid)) \
order by case "libertybans_applicable_bans"."end" \
when 0 then 9223372036854775807 else "libertybans_applicable_bans"."end" end desc limit 1""";
			case STERN -> """
select "libertybans_applicable_bans"."victim_type", \
"libertybans_applicable_bans"."victim_uuid", "libertybans_applicable_bans"."victim_address", \
"libertybans_applicable_bans"."operator", "libertybans_applicable_bans"."reason", \
"libertybans_applicable_bans"."scope_type", "libertybans_applicable_bans"."scope", "libertybans_applicable_bans"."start", \
"libertybans_applicable_bans"."end", "libertybans_applicable_bans"."track", "libertybans_applicable_bans"."id" \
from "libertybans_applicable_bans" \
join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1" \
where (("libertybans_applicable_bans"."end" = 0 or "libertybans_applicable_bans"."end" > cast(? as bigint)) \
and \
("libertybans_strict_links"."uuid1" = cast(? as uuid) or ("libertybans_strict_links"."uuid2" = cast(? as uuid) \
and "libertybans_applicable_bans"."victim_type" <> 0))) \
order by case "libertybans_applicable_bans"."end" \
when 0 then 9223372036854775807 else "libertybans_applicable_bans"."end" end desc limit 1""";
			case STRICT -> """
select "libertybans_applicable_bans"."victim_type", \
"libertybans_applicable_bans"."victim_uuid", "libertybans_applicable_bans"."victim_address", \
"libertybans_applicable_bans"."operator", "libertybans_applicable_bans"."reason", \
"libertybans_applicable_bans"."scope_type", "libertybans_applicable_bans"."scope", "libertybans_applicable_bans"."start", \
"libertybans_applicable_bans"."end", "libertybans_applicable_bans"."track", "libertybans_applicable_bans"."id" \
from "libertybans_applicable_bans" \
join "libertybans_strict_links" on "libertybans_applicable_bans"."uuid" = "libertybans_strict_links"."uuid1" \
where (("libertybans_applicable_bans"."end" = 0 or "libertybans_applicable_bans"."end" > cast(? as bigint)) \
and \
"libertybans_strict_links"."uuid2" = cast(? as uuid)) \
order by case "libertybans_applicable_bans"."end" \
when 0 then 9223372036854775807 else "libertybans_applicable_bans"."end" end desc limit 1""";
		};
	}

}
