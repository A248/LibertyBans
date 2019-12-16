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

import java.io.File;

public class ConfigLoadException extends InternalStateException {

	private static final long serialVersionUID = -6838417590106589911L;

	public ConfigLoadException(File file) {
		super("File " + file.getPath() + " is invalid!");
	}
	
	public ConfigLoadException(File file, Exception cause) {
		super("File " + file.getPath() + " is invalid!", cause);
	}
	
	public ConfigLoadException(String file, Exception cause) {
		super("Configuration for " + file + " encountered an error!", cause);
	}

}
