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

package space.arim.libertybans.core.database;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.punish.GlobalEnforcement;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.nio.file.Path;

@Singleton
public class DatabaseManager implements Part {

	private final Path folder;
	private final FactoryOfTheFuture futuresFactory;
	private final EnhancedExecutor enhancedExecutor;
	private final Configs configs;
	private final Time time;
	private final GlobalEnforcement globalEnforcement;

	private volatile StandardDatabase database;

	@Inject
	public DatabaseManager(@Named("folder") Path folder, FactoryOfTheFuture futuresFactory,
						   EnhancedExecutor enhancedExecutor, Configs configs, Time time,
						   GlobalEnforcement globalEnforcement) {
		this.folder = folder;
		this.futuresFactory = futuresFactory;
		this.enhancedExecutor = enhancedExecutor;
		this.configs = configs;
		this.time = time;
		this.globalEnforcement = globalEnforcement;
	}

	public FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}

	EnhancedExecutor enhancedExecutor() {
		return enhancedExecutor;
	}

	Configs configs() {
		return configs;
	}

	GlobalEnforcement globalEnforcement() {
		return globalEnforcement;
	}

	public InternalDatabase getInternal() {
		return database;
	}
	
	public PunishmentDatabase getExternal() {
		return database.asExternal();
	}
	
	@Override
	public void startup() {
		DatabaseResult dbResult = new DatabaseSettings(folder, this).create();
		StandardDatabase database = dbResult.database();
		if (!dbResult.success()) {
			database.closeCompletely();
			throw new StartupException("Database initialisation failed");
		}
		dbResult.preinitializeJooqClasses();
		database.startTasks(time);
		this.database = database;
	}
	
	@Override
	public void restart() {
		StandardDatabase currentDatabase = this.database;

		DatabaseResult dbResult = new DatabaseSettings(folder, this).create();
		StandardDatabase database = dbResult.database();
		if (!dbResult.success()) {
			database.close();
			throw new StartupException("Database restart failed");
		}
		currentDatabase.cancelTasks();

		if (currentDatabase.getVendor() == database.getVendor()) {
			currentDatabase.close();
		} else {
			currentDatabase.closeCompletely();
			dbResult.preinitializeJooqClasses();
		}

		database.startTasks(time);
		this.database = database;
	}

	@Override
	public void shutdown() {
		StandardDatabase database = this.database;
		database.cancelTasks();
		database.closeCompletely();
	}
	
}
