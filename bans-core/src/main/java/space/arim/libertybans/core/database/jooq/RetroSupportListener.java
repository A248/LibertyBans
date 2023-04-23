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

package space.arim.libertybans.core.database.jooq;

import org.jetbrains.annotations.NotNull;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.ExecuteType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RetroSupportListener implements ExecuteListener {

	private static final Pattern OFFSET_FETCH_PATTERN = Pattern.compile(
			"OFFSET (\\?|[0-9]+) ROWS FETCH (FIRST|NEXT) (\\?|[0-9]+) ROWS ONLY", Pattern.CASE_INSENSITIVE
	);
	private static final Pattern OFFSET_PATTERN = Pattern.compile(
			"OFFSET (\\?|[0-9]+) ROWS", Pattern.CASE_INSENSITIVE
	);
	private static final Pattern FETCH_PATTERN = Pattern.compile(
			"FETCH (FIRST|NEXT) (\\?|[0-9]+) ROWS ONLY", Pattern.CASE_INSENSITIVE
	);

	@Override
	public void prepareStart(ExecuteContext context) {
		if (context.type() != ExecuteType.READ) {
			return;
		}
		String query = context.sql();
		if (query == null) {
			return;
		}
		StringBuilder newQuery = new StringBuilder(query.length());
		Matcher matcher1, matcher2, matcher3;
		matcher1 = OFFSET_FETCH_PATTERN.matcher(query);
		if (matcher1.find()) {
			// MariaDB thankfully provides the syntax 'LIMIT offset, row_count'
			matcher1.appendReplacement(newQuery, "LIMIT $1, $3");
			matcher1.appendTail(newQuery);
		} else if ((matcher2 = OFFSET_PATTERN.matcher(query)).find()) {
			matcher2.appendReplacement(newQuery, "LIMIT " + Integer.MAX_VALUE + " OFFSET $1");
			matcher2.appendTail(newQuery);
		} else if ((matcher3 = FETCH_PATTERN.matcher(query)).find()) {
			matcher3.appendReplacement(newQuery, "LIMIT $2");
			matcher3.appendTail(newQuery);
		} else {
			// Keep the existing query
			return;
		}
		context.sql(newQuery.toString());
	}

	class Provider implements ExecuteListenerProvider {

		@Override
		public @NotNull ExecuteListener provide() {
			return RetroSupportListener.this;
		}

	}

}
