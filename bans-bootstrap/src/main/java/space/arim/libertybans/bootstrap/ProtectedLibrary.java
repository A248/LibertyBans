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

public enum ProtectedLibrary {

	HSQLDB("HSQLDB", "org.hsqldb", "jdbc.JDBCDriver"),
	MARIADB_CONNECTOR("MariaDB-Connector", "org.mariadb.jdbc", "Driver"),
	PGJDBC("PGJDBC", "org.postgresql", "Driver"),

	HIKARICP("HikariCP", "com.zaxxer.hikari", "HikariConfig"),
	FLYWAY("Flyway", "org.flywaydb", "core.Flyway"),
	JOOQ("JOOQ", "org.jooq", "DSLContext"),

	DAZZLECONF_CORE("DazzleConf-Core", "space.arim.dazzleconf", "ConfigurationFactory"),
	DAZZLECONF_EXT_SNAKEYAML("DazzleConf-SnakeYaml",
			"space.arim.dazzleconf.ext.snakeyaml", "SnakeYamlConfigurationFactory"),

	JAKARTA_INJECT("Jakarta-Inject", "jakarta.inject", "Provider"),
	SOLID_INJECTOR("SolidInjector", "space.arim.injector", "Injector"),

	CAFFEINE("Caffeine", "com.github.benmanes.caffeine.cache", "Caffeine"),
	KYORI_ADVENTURE("Kyori-Adventure", "net.kyori.adventure", "audience.Audience"),
	KYORI_EXAMINATION("Kyori-Examination", "net.kyori.examination", "Examinable"),
	SLF4J_API("Slf4j", "org.slf4j", "Logger"),
	SLF4J_SIMPLE("Slf4j-Simple", "org.slf4j.simple", "SimpleLogger");

	private final String libName;
	private final String basePackage;
	private final String sampleClassName;

	ProtectedLibrary(String libraryName, String basePackage, String sampleClassName) {
		this.libName = libraryName;
		this.basePackage = basePackage;
		this.sampleClassName = sampleClassName;
	}

	/**
	 * A display name for the library
	 * @return the library name
	 */
	String libraryName() {
		return libName;
	}

	/**
	 * The base package of classes from the library
	 * @return the base package
	 */
	String basePackage() {
		return basePackage;
	}

	/**
	 * A sample class from the library used to detect mistaken shading without relocation
	 * @return a sample class name
	 */
	String sampleClass() {
		return basePackage + '.' + sampleClassName;
	}

	/**
	 * Determines whether this library is present in the given classloader or any of its
	 * parent classloaders
	 *
	 * @param expectedClassLoader the classloader to check in
	 * @return true if the library is present in the classloader or any of its parents
	 */
	boolean detect(ClassLoader expectedClassLoader) {
		Class<?> sampleClass;
		try {
			sampleClass = Class.forName(sampleClass());
		} catch (ClassNotFoundException ex) {
			return false;
		}
		return findClassLoaderInHierarchy(sampleClass.getClassLoader(), expectedClassLoader);
	}

	private static boolean findClassLoaderInHierarchy(ClassLoader subjectToLookFor, ClassLoader hierarchy) {
		if (subjectToLookFor == hierarchy) {
			return true;
		}
		ClassLoader parent = hierarchy.getParent();
		return parent != null && findClassLoaderInHierarchy(subjectToLookFor, parent);
	}

	@Override
	public String toString() {
		return libName;
	}
}
