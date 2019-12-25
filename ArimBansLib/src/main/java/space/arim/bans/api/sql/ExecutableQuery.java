/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
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
package space.arim.bans.api.sql;

import space.arim.bans.api.util.StringsUtil;

import space.arim.universal.util.collections.CollectionsUtil;

public class ExecutableQuery {

	private final String statement;
	private final Object[] parameters;
	
	public ExecutableQuery(String statement, Object...parameters) {
		this.statement = statement;
		this.parameters = parameters;
	}
	
	public String statement() {
		return statement;
	}
	
	public Object[] parameters() {
		return parameters;
	}
	
	@Override
	public String toString() {
		return "{[" + statement + "] with parameters [" + StringsUtil.concat(CollectionsUtil.convertAll(parameters, (param) -> param.toString()), ',') + "]}";
	}
	
}
