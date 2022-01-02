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

package space.arim.libertybans.core.database.flyway;

import org.jooq.DSLContext;

import java.util.List;
import java.util.Objects;

final class Schema08x {

	private final String tablePrefix;
	private final DSLContext context;

	Schema08x(String tablePrefix, DSLContext context) {
		this.tablePrefix = Objects.requireNonNull(tablePrefix, "tablePrefix");
		this.context = context;
	}

	void dropViews() {
		List<String> views08x = List.of( // Order is significant
				"applicable_bans", "applicable_mutes", "applicable_warns", "strict_links",
				"latest_names", "latest_addresses",
				"simple_active", "simple_bans", "simple_mutes", "simple_warns", "simple_history"
		);
		for (String view : views08x) {
			context.dropView(view).execute();
		}
	}

	void renameTables(String newPrefix) {
		List<String> tables08x = List.of("names", "addresses", "punishments", "history", "bans", "mutes", "warns");
		for (String table : tables08x) {
			context.alterTable(tablePrefix + table)
					.renameTo(newPrefix + table)
					.execute();
		}
	}
}
