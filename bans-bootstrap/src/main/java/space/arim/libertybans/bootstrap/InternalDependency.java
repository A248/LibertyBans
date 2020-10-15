/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import space.arim.libertybans.bootstrap.depend.Repository;

enum InternalDependency {

	HIKARICP("HikariCP", "com.zaxxer.hikari.HikariConfig", "hikaricp"),
	
	HSQLDB("HSQLDB", "org.hsqldb.jdbc.JDBCDriver", "hsqldb"),
	MARIADB("MariaDB-Connector", "org.mariadb.jdbc.Driver", "mariadb-connector"),
	
	CAFFEINE("Caffeine", "com.github.benmanes.caffeine.cache.Caffeine", "caffeine"),
	
	JDBCAESAR("jdbcaesar", Repositories.ARIM_LESSER_GPL3),
	DAZZLECONF_CORE("dazzleconf-core", Repositories.ARIM_LESSER_GPL3),
	DAZZLECONF_EXT_SNAKEYAML("dazzleconf-ext-snakeyaml", Repositories.ARIM_LESSER_GPL3),
	ARIMAPI("arimapi", Repositories.ARIM_GPL3),
	MOREPAPERLIB("morepaperlib", Repositories.ARIM_LESSER_GPL3),
	
	SELF_CORE("self-core", Repositories.ARIM_AFFERO_GPL3);
	
	final String name;
	final String clazz;
	final String id;
	final Repository repo;
	
	private InternalDependency(String name, String clazz, String id, Repository repo) {
		this.name = name;
		this.clazz = clazz;
		this.id = id;
		this.repo = repo;
	}
	
	private InternalDependency(String name, String clazz, String id) {
		this(name, clazz, id, Repositories.CENTRAL_REPO);
	}
	
	private InternalDependency(String id, Repository repo) {
		this(null, null, id, repo);
	}
	
}
