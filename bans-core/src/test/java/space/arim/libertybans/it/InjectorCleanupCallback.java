/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import space.arim.injector.Injector;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.sync.SQLSynchronizationMessenger;
import space.arim.libertybans.core.service.SettableTime;

final class InjectorCleanupCallback implements AfterEachCallback {

	private final Injector injector;

	InjectorCleanupCallback(Injector injector) {
		this.injector = injector;
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		// Reset database
		injector.request(InternalDatabase.class).truncateAllTables();
		// Reset global clock
		injector.request(SettableTime.class).reset();
		// Reset synchronization
		injector.request(SQLSynchronizationMessenger.class).resetLastTimestamp();
	}
}
