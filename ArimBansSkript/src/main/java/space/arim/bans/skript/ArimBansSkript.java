/* 
 * ArimBansSkript, a skript addon for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansSkript is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansSkript is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansSkript. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.skript;

import space.arim.bans.api.ArimBansLibrary;

public class ArimBansSkript implements AutoCloseable {
	
	private final ArimBansLibrary lib;
	
	ArimBansSkript(ArimBansLibrary lib) {
		this.lib = lib;
	}
	
	void registerAll() {
		
	}

	@Override
	public void close() {
		
	}
	
}
