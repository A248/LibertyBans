/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import space.arim.bans.internal.Component;

import space.arim.api.sql.ExecutableQuery;

public interface SqlMaster extends Component {
	@Override
	default Class<?> getType() {
		return SqlMaster.class;
	}
	
	String getStorageModeName();
	
	boolean enabled();
	
	ResultSet[] execute(ExecutableQuery...queries) throws SQLException;
	
	ResultSet[] execute(Query...queries); 
	
	default ResultSet[] execute(Collection<Query> queries) {
		return execute(queries.toArray(new Query[] {}));
	}

}
