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
package space.arim.bans;

import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.env.bungee.BungeeEnv;

public class ArimBansBungee extends Plugin implements AutoCloseable {

	private ArimBans center;
	private BungeeEnv environment;
	
	private void load() {
		environment = new BungeeEnv(this);
		center = new ArimBansPlugin(getDataFolder(), environment);
		center.start();
		center.register();
		environment.setCenter(center);
	}
	
	@Override
	public void close() {
		center.close();
		environment.close();
	}
	
	@Override
	public void onEnable() {
		load();
	}

	@Override
	public void onDisable() {
		close();
	}
	
}
