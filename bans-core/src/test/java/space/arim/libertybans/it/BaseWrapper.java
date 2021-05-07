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

import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import space.arim.injector.Injector;
import space.arim.libertybans.bootstrap.BaseFoundation;

import java.nio.file.Path;

class BaseWrapper implements CloseableResource {

	final Injector injector;
	private final BaseFoundation base;
	private final Path tempDirectory;

	BaseWrapper(Injector injector, BaseFoundation base, Path tempDirectory) {
		this.injector = injector;
		this.base = base;
		this.tempDirectory = tempDirectory;
	}

	@Override
	public void close() throws Throwable {
		base.shutdown();
		new ClosablePath(tempDirectory).close();
	}

}
