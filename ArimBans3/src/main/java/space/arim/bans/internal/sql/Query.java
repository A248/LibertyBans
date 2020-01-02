/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.sql;

import space.arim.api.sql.ExecutableQuery;

public interface Query {

	default ExecutableQuery convertToExecutable(SqlSettings settings) {
		String statement = settings.toString().equals("mysql") ? statement() : statement().replace(",PRIMARY KEY (`id`))", ")").replace(" AUTO_INCREMENT", " IDENTITY PRIMARY KEY").replace(" int ", " INTEGER ").replace(" NOT NULL", "").replace("`", "").replace("TEXT", "CLOB");
		return new ExecutableQuery(statement.replace("%PREFIX%", settings.prefix), parameters());
	}
	
	String statement();
	
	Object[] parameters();
	
}
