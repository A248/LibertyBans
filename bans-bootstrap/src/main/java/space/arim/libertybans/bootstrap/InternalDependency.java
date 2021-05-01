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

import space.arim.libertybans.bootstrap.depend.Dependency;
import space.arim.libertybans.bootstrap.depend.LocatableDependency;
import space.arim.libertybans.bootstrap.depend.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

enum InternalDependency {
	
	HSQLDB("HSQLDB", "org.hsqldb.jdbc.JDBCDriver", "hsqldb"),
	MARIADB("MariaDB-Connector", "org.mariadb.jdbc.Driver", "mariadb-connector"),
	
	HIKARICP("HikariCP", "com.zaxxer.hikari.HikariConfig", "hikaricp"),
	FLYWAY("Flyway", "org.flywaydb.core.Flyway", "flyway"),
	CAFFEINE("Caffeine", "com.github.benmanes.caffeine.cache.Caffeine", "caffeine"),
	
	DAZZLECONF_CORE("DazzleConf-Core", "space.arim.dazzleconf.ConfigurationFactory", "dazzleconf-core"),
	DAZZLECONF_EXT_SNAKEYAML("DazzleConf-SnakeYaml",
			"space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory", "dazzleconf-ext-snakeyaml"),
	
	JAKARTA_INJECT("Jakarta-Inject", "jakarta.inject.Provider", "jakarta-inject"),
	SOLID_INJECTOR("SolidInjector", "space.arim.injector.Injector", "solidinjector"),

	/*
	JDBCAESAR("jdbcaesar", Repositories.ARIM_LESSER_GPL3),
	ARIMAPI("arimapi", Repositories.ARIM_GPL3),
	MANAGED_WAITS("managed-waits", Repositories.ARIM_GPL3),
	MOREPAPERLIB("morepaperlib", Repositories.ARIM_LESSER_GPL3),
	*/
	
	SELF_IMPLEMENTATION("self-implementation", Repositories.ARIM_AFFERO_GPL3),

	SLF4J_API("slf4j-api", Repositories.CENTRAL_REPO),
	SLF4J_JUL("slf4j-jdk14", Repositories.CENTRAL_REPO);

	private final ClassPresence classPresence;
	private final String id;
	private final Repository repository;
	
	private InternalDependency(ClassPresence classPresence, String id, Repository repository) {
		this.classPresence = classPresence;
		this.id = id;
		this.repository = repository;
	}
	
	private InternalDependency(String name, String clazz, String id) {
		this(new ClassPresence(clazz, name), id, Repositories.CENTRAL_REPO);
	}
	
	private InternalDependency(String id, Repository repository) {
		this(null, id, repository);
	}

	Optional<ClassPresence> classPresence() {
		return Optional.ofNullable(classPresence);
	}

	CompletableFuture<LocatableDependency> locateUsing(Executor executor) {
		return CompletableFuture.supplyAsync(this::readDependency, executor)
				.thenApply((dependency) -> new LocatableDependency(dependency, repository));
	}

	private Dependency readDependency() {
		String resourcePath = "/dependencies/" + id;
		URL url = getClass().getResource(resourcePath);
		Objects.requireNonNull(url, "internal error, missing " + resourcePath);
		try (InputStream inputStream = url.openStream()) {

			String fullString = new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
			String[] lines = fullString.lines().toArray(String[]::new);
			if (lines.length < 4) {
				throw new IllegalArgumentException("Dependency file for " + id + " is malformatted");
			}
			return Dependency.of(lines[0], lines[1], lines[2], lines[3]);

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
	
}
