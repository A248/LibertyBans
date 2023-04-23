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

import org.jooq.ExecuteContext;
import org.jooq.ExecuteType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RetroSupportListenerTest {

	private final RetroSupportListener listener = new RetroSupportListener();

	private String modifyQuery(String sql) {
		class Holder {
			String newSql = sql;
		}
		Holder holder = new Holder();

		ExecuteContext executeContext = mock(ExecuteContext.class);
		when(executeContext.type()).thenReturn(ExecuteType.READ);
		when(executeContext.sql()).thenReturn(sql);

		lenient().doAnswer((Answer<Void>) invocation -> {
			holder.newSql = invocation.getArgument(0, String.class);
			return null;
		}).when(executeContext).sql(any());

		listener.prepareStart(executeContext);

		return holder.newSql;
	}

	private void assertModification(String transformed, String original) {
		assertEquals(transformed, modifyQuery(original));
	}

	@Test
	public void querySimply() {
		assertModification(
				"SELECT col FROM tab",
				"SELECT col FROM tab"
		);
	}

	@Test
	public void queryWithOffset() {
		assertModification(
				"SELECT col FROM tab LIMIT " + Integer.MAX_VALUE + " OFFSET 4",
				"SELECT col FROM tab OFFSET 4 ROWS"
		);
	}

	@Test
	public void queryWithOffsetLimit() {
		assertModification(
				"SELECT col FROM tab LIMIT 38 OFFSET 4",
				"SELECT col FROM tab LIMIT 38 OFFSET 4"
		);
	}

	@Test
	public void queryWithOffsetFetchFirst() {
		assertModification(
				"SELECT col FROM tab LIMIT 4, 38",
				"SELECT col FROM tab OFFSET 4 ROWS FETCH FIRST 38 ROWS ONLY"
		);
	}

	@Test
	public void queryWithFetchFirst() {
		assertModification(
				"SELECT col FROM tab LIMIT 38",
				"SELECT col FROM tab FETCH FIRST 38 ROWS ONLY"
		);
	}

	@Test
	public void queryWithOffsetFetchFirstParameters() {
		assertModification(
				"SELECT col FROM tab LIMIT ?, ?",
				"SELECT col FROM tab OFFSET ? ROWS FETCH FIRST ? ROWS ONLY"
		);
	}

	@Test
	public void queryWithFetchFirstParameter() {
		assertModification(
				"SELECT col FROM tab LIMIT ?",
				"SELECT col FROM tab FETCH FIRST ? ROWS ONLY"
		);
	}

}
