/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.it.env.QuackEnv;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;

public class ProfligateInstanceProvider implements ArgumentsProvider {

	private static DB mariaDb;
	private static final AtomicInteger dbNameCounter = new AtomicInteger();
	
	private static final Class<?> THIS_CLASS = MethodHandles.lookup().lookupClass();
	private static final Namespace NAMESPACE = Namespace.create(THIS_CLASS);
	private static final Logger logger = LoggerFactory.getLogger(THIS_CLASS);
	
	private static synchronized DB getMariaDb() {
		if (mariaDb == null) {
			try {
				DB db = DB.newEmbeddedDB(0);
				db.start();
				shutdownHook(() -> {
					try {
						db.stop();
					} catch (ManagedProcessException ex) {
						logger.warn("Unable to shutdown MariaDb4j", ex);
					}
				});
				mariaDb = db;

			} catch (ManagedProcessException ex) {
				Assertions.fail(ex);
				throw new RuntimeException(ex);
			}
		}
		return mariaDb;
	}
	
	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
		Set<ConfigSpec> possibilities = ConfigSpec.getAll();
		AnnotatedElement element = context.getElement().orElse(null);
		if (element != null) {
			ConfigConstraints constraints = element.getAnnotation(ConfigConstraints.class);
			if (constraints != null) {
				possibilities.removeIf((possibility) -> !possibility.agrees(constraints));
			}
		}
		return possibilities.stream().map((spec) -> fromSpec(spec, context)).map(Arguments::of);
	}
	
	private LibertyBansCore fromSpec(ConfigSpec spec, ExtensionContext context) {

		if (spec.getVendor() == Vendor.MARIADB) {
			int port = getMariaDb().getConfiguration().getPort();
			String database = "libertybans_it_" + dbNameCounter.getAndIncrement();
			createDatabase(port, database);
			spec = new ConfigSpec(spec.getVendor(), spec.getAddressStrictness(), port, database);
		}
		Path tempDir = createTemporaryDirectory();
		QuackEnv env = new QuackEnv(tempDir, spec);
		env.startup();
		context.getStore(NAMESPACE).put("profligate-instance", env);
		return env.core();
	}
	
	private static void createDatabase(int port, String database) {
		try (Connection conn = DriverManager.getConnection("jdbc:mariadb://localhost:" + port + '/', "root", "");
				PreparedStatement prepStmt = conn.prepareStatement("CREATE DATABASE " + database)) {

			prepStmt.execute();
		} catch (SQLException ex) {
			Assertions.fail(ex);
			throw new RuntimeException(ex);
		}
	}
	
	private static Path createTemporaryDirectory() {
		Path tempDir;
		try {
			tempDir = Files.createTempDirectory("libertybans-test-mariadb4j");
		} catch (IOException ex) {
			Assertions.fail(ex);
			throw new RuntimeException(ex);
		}

		shutdownHook(() -> {
			try (Stream<Path> walk = Files.walk(tempDir)) {
				walk.sorted(Comparator.reverseOrder())
					.forEach((path) -> {
						try {
							Files.delete(path);
						} catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					});
			} catch (IOException | UncheckedIOException ex) {
				logger.warn("Unable to delete temporary directory", ex);
			}
		});
		return tempDir;
	}
	
	private static void shutdownHook(Runnable toDo) {
		Runtime.getRuntime().addShutdownHook(new Thread(toDo));
	}

}
