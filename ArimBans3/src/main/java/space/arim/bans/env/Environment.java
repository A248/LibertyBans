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
package space.arim.bans.env;

import java.util.UUID;
import java.util.logging.Logger;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.internal.Configurable;

public interface Environment extends Configurable {
	
	void loadFor(ArimBans center);
	
	boolean isOnlineMode();
	
	boolean isOnline(Subject subject);
	
	void sendMessage(Subject target, String jsonable, boolean useJson);
	
	void sendMessage(String permission, String jsonable, boolean useJson);
	
	boolean hasPermission(Subject subject, String permission, boolean opPerms);
	
	void enforce(Punishment punishment, boolean useJson);
	
	Logger logger();
	
	UUID uuidFromName(String name);
	
	String nameFromUUID(UUID uuid);
	
	String getName();
	
	String getAuthor();
	
	String getVersion();

}