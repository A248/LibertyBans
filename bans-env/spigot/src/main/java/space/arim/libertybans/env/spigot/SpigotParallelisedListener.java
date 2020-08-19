/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import space.arim.libertybans.core.env.ParallelisedListener;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

class SpigotParallelisedListener<E, R> extends ParallelisedListener<E, R> implements Listener {

	final SpigotEnv env;
	
	SpigotParallelisedListener(SpigotEnv env) {
		this.env = env;
	}
	
	@Override
	public void register() {
		env.getPlugin().getServer().getPluginManager().registerEvents(this, env.getPlugin());
	}

	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
}
