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
import java.util.function.UnaryOperator;

final class Rename08xTables {

	private final String tablePrefix;
	private final UnaryOperator<String> renaming;

	Rename08xTables(String tablePrefix, UnaryOperator<String> renaming) {
		this.tablePrefix = Objects.requireNonNull(tablePrefix, "tablePrefix");
		this.renaming = Objects.requireNonNull(renaming, "renaming");
	}

	void rename(DSLContext context) {
		List<String> tables08x = List.of("names", "addresses", "punishments", "history", "bans", "mutes", "warns");
		for (String table : tables08x) {
			context.alterTable(tablePrefix + table)
					.renameTo(renaming.apply(table))
					.execute();
		}
	}
}
