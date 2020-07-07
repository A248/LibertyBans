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
package space.arim.libertybans.bootstrap;

/**
 * Exception signalling a startup failure. Stacktrace of this exception need not be printed. <br>
 * It is intended to stop processing a startup in which an actual error has already been identified.
 * 
 * @author A248
 *
 */
public class StartupException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -442174740832379722L;
	
	public StartupException() {
		
	}
	
	public StartupException(String message) {
		super(message);
	}
	
	public StartupException(Throwable cause) {
		super(cause);
	}
	
	public StartupException(String message, Throwable cause) {
		super(message, cause);
	}

}
