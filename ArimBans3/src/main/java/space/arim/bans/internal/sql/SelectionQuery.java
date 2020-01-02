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

public class SelectionQuery implements Query {
	
	private final String table;
	
	private String qualifications = "";
	private Object[] parameters;
	
	public SelectionQuery(String table) {
		this.table = table;
	}
	
	@Override
	public String statement() {
		String base = "SELECT * FROM `%PREFIX%" + table + "`";
		return qualifications.isEmpty() ? base : base + " WHERE " + qualifications;
	}
	
	@Override
	public Object[] parameters() {
		return parameters;
	}
	
	public static SelectionQuery create(String table) {
		return new SelectionQuery(table);
	}
	
	private void addQualification(String qualification) {
		qualifications = qualifications.isEmpty() ? qualification : qualifications + " AND " + qualification;
	}
	
	private void addParameter(Object parameter) {
		if (parameters == null) {
			parameters = new Object[] {parameter};
			return;
		}
		Object[] oldParams = parameters;
		parameters = new Object[oldParams.length + 1];
		parameters[parameters.length - 1] = parameter;
	}
	
	public SelectionQuery addCondition(String identifier, String comparison, Object parameter) {
		addQualification("`" + identifier + "` " + comparison + " ?");
		addParameter(parameter);
		return this;
	}
	
	public SelectionQuery addCondition(String identifier, Object parameter) {
		return addCondition(identifier, "=", parameter);
	}
	
}
