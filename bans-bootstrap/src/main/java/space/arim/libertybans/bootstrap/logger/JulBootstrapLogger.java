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
package space.arim.libertybans.bootstrap.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class JulBootstrapLogger implements BootstrapLogger {

	private final Logger logger;
	
	public JulBootstrapLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void warn(String message) {
		logger.warning(message);
	}

	@Override
	public void warn(String message, Throwable ex) {
		logger.log(Level.WARNING, message, ex);
	}
	
	@Override
	public void error(String message) {
		logger.severe(message);
	}

}
