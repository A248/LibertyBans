/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;

public class DatabaseManager implements Part {

	private final LibertyBansCore core;
	
	private volatile Database database;
	
	public DatabaseManager(LibertyBansCore core) {
		this.core = core;
	}
	
	public Database getCurrentDatabase() {
		return database;
	}
	
	@Override
	public void startup() {
		DatabaseResult dbResult = new DatabaseSettings(core).create().join();
		Database database = dbResult.database;
		if (!dbResult.success) {
			database.closeCompletely();
			throw new StartupException("Database initialisation failed");
		}
		database.startRefreshTaskIfNecessary();
		this.database = database;
	}
	
	@Override
	public void restart() {
		Database currentDatabase = this.database;

		DatabaseResult dbResult = new DatabaseSettings(core).create().join();
		Database database = dbResult.database;
		if (!dbResult.success) {
			database.close();
			throw new StartupException("Database restart failed");
		}
		currentDatabase.cancelRefreshTaskIfNecessary();

		if (currentDatabase.getVendor() == database.getVendor()) {
			currentDatabase.close();
		} else {
			currentDatabase.closeCompletely();
		}

		database.startRefreshTaskIfNecessary();
		this.database = database;
	}

	@Override
	public void shutdown() {
		Database database = this.database;
		database.cancelRefreshTaskIfNecessary();
		database.closeCompletely();
	}
	
}
