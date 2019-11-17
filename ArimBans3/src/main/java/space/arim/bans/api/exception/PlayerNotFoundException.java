/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.exception;

import java.util.UUID;

public class PlayerNotFoundException extends InternalAPIException {

	
	private static final long serialVersionUID = 1659138619614361712L;
	
	public PlayerNotFoundException(UUID playeruuid) {
		super("Player by uuid " + playeruuid.toString() + " could not be found through the cache, internal Bukkit API, nor external Mojang API.");
	}
	public PlayerNotFoundException(String name) {
		super("Player by name " + name + " could not be found through the cache, internal Bukkit API, nor external Mojang API.");
	}

}
