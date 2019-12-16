/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.exception;

public class ConfigSectionException extends InternalStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3818324615465756966L;
	
	/**
     * Constructs a <code>ConfigSectionException</code> with the
     * specified YAML key.
     *
     * @param   key   the config section key.
     */
	public ConfigSectionException(String key) {
		super("Error in config section " + key + ".");
	}
	
	/**
     * Constructs a <code>ConfigSectionException</code> with the
     * specified YAML key and cause
     *
     * @param   key    the config section key.
     * @param   cause  the exception for this exception
     */
	public ConfigSectionException(String key, Exception cause) {
		super("Error in config section " + key + ".", cause);
	}
}
