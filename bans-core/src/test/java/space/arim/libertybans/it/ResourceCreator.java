/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.it;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import space.arim.injector.Identifier;
import space.arim.injector.Injector;
import space.arim.injector.InjectorBuilder;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.PillarOneReplacementModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.it.env.QuackBindModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

class ResourceCreator {

	private final Store store;

	private static final AtomicInteger DB_NAME_COUNTER = new AtomicInteger();

	ResourceCreator(Store store) {
		this.store = store;
	}

	Injector create(ConfigSpec configSpec) {

		int mariaDbPort = computeMariaDbPort(configSpec.vendor());

		return store.getOrComputeIfAbsent(configSpec, (spec) -> {

			DatabaseInfo databaseInfo;
			if (spec.vendor() == Vendor.MARIADB) {
				String database = "libertybans_it_" + DB_NAME_COUNTER.incrementAndGet();
				createDatabase(mariaDbPort, database);
				databaseInfo = new DatabaseInfo(mariaDbPort, database);
			} else {
				databaseInfo = new DatabaseInfo();
			}

			Path tempDirectory = createTempDirectory();

			Injector injector = new InjectorBuilder()
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), tempDirectory)
				.bindInstance(ConfigSpec.class, spec)
				.bindInstance(DatabaseInfo.class, databaseInfo)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneReplacementModule(),
						new PillarTwoBindModule(),
						new QuackBindModule())
				.build();

			BaseFoundation base = injector.request(BaseFoundation.class);
			base.startup();
			return new BaseWrapper(injector, base, tempDirectory);

		}, BaseWrapper.class).injector;
	}

	private static void createDatabase(int port, String database) {
		try (Connection conn = DriverManager.getConnection("jdbc:mariadb://localhost:" + port + '/', "root", "");
				PreparedStatement prepStmt = conn.prepareStatement("CREATE DATABASE " + database
						+ " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")) {

			prepStmt.execute();
		} catch (SQLException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
	}

	private static Path createTempDirectory() {
		Path tempDir;
		try {
			tempDir = Files.createTempDirectory("libertybans-test-dir");
		} catch (IOException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
		return tempDir;
	}

	private int computeMariaDbPort(Vendor vendor) {
		if (vendor != Vendor.MARIADB) {
			return 0;
		}
		ClosableMariaDb closableMariaDb = store.getOrComputeIfAbsent(vendor, (v) -> {
			DB db;
			try {
				db = DB.newEmbeddedDB(0);
				db.start();
			} catch (ManagedProcessException ex) {
				throw Assertions.<RuntimeException>fail(ex);
			}
			return new ClosableMariaDb(db);
		}, ClosableMariaDb.class);

		return closableMariaDb.db.getConfiguration().getPort();
	}

}
