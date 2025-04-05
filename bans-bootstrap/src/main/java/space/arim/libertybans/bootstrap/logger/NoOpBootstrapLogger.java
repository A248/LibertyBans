/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

public class NoOpBootstrapLogger implements BootstrapLogger {
    @Override
    public void debug(String message) {

    }

    @Override
    public void info(String message) {

    }

    @Override
    public void warn(String message) {
        throw new RuntimeException("WARN will not be swallowed. Message: " + message);
    }

    @Override
    public void warn(String message, Throwable ex) {
        throw new RuntimeException("WARN with exception will not be swallowed. Message: " + message, ex);
    }

    @Override
    public void error(String message) {
        throw new RuntimeException("ERROR will not be swallowed. Message: " + message);
    }
}
