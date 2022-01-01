/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.bootstrap;

enum RelocationCheck {

	HSQLDB("HSQLDB", "org.hsqldb.jdbc.JDBCDriver"),
	MARIADB_CONNECTOR("MariaDB-Connector", "org.mariadb.jdbc.Driver"),
	PGJDBC("PGJDBC", "org.postgresql.Driver"),

	HIKARICP("HikariCP", "com.zaxxer.hikari.HikariConfig"),
	FLYWAY("Flyway", "org.flywaydb.core.Flyway"),
	
	DAZZLECONF_CORE("DazzleConf-Core", "space.arim.dazzleconf.ConfigurationFactory"),
	DAZZLECONF_EXT_SNAKEYAML("DazzleConf-SnakeYaml",
			"space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory"),
	
	JAKARTA_INJECT("Jakarta-Inject", "jakarta.inject.Provider"),
	SOLID_INJECTOR("SolidInjector", "space.arim.injector.Injector");

	private final String libName;
	private final String className;

	RelocationCheck(String libName, String className) {
		this.libName = libName;
		this.className = className;
	}

	String libName() {
		return libName;
	}

	String className() {
		return className;
	}

	@Override
	public String toString() {
		return libName;
	}
}
