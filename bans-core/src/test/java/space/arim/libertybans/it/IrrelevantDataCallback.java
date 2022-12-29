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

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import space.arim.injector.Identifier;
import space.arim.injector.Injector;
import space.arim.libertybans.core.importing.SelfImportProcess;
import space.arim.libertybans.it.test.importing.SelfImportData;

import java.nio.file.Path;

final class IrrelevantDataCallback implements BeforeEachCallback {

	private final Injector injector;

	IrrelevantDataCallback(Injector injector) {
		this.injector = injector;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Path folder = injector.request(Identifier.ofTypeAndNamed(Path.class, "folder"));
		SelfImportProcess selfImportProcess = injector.request(SelfImportProcess.class);
		// Currently, we simply import BlueTree's existing database
		Path blueTreeFolder = new SelfImportData(folder).copyBlueTree242();
		selfImportProcess.transferAllData(blueTreeFolder).join();
	}

}
