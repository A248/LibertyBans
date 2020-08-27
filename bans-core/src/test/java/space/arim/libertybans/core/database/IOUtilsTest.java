/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

public class IOUtilsTest {

	@Test
	public void testReadResource() {
		assertEquals("random content", IOUtils.readResource("whatever.yml").toString(StandardCharsets.UTF_8));

		String enactmentProcedure = IOUtils.readResource("sql/procedure_banhammer.sql").toString(StandardCharsets.UTF_8);
		System.out.println(enactmentProcedure);
		assertFalse(enactmentProcedure.isBlank());
	}
	
	@Test
	public void testReadQueries() {
		assertEquals(List.of(
				"CREATE TABLE myTable (\n" + 
				"myKey INT AUTO_INCREMENT PRIMARY KEY,\n" + 
				"value VARCHAR(20) NOT NULL)",
				"INSERT INTO myTable (value) VALUES ('another query')",
				"CREATE VIEW idkView AS SELECT * FROM myTable WHERE value = 'idk'"
				), IOUtils.readSqlQueries("queries.sql"));
		assertEquals(4, IOUtils.readSqlQueries("sql/create_views.sql").size());
	}
	
}
