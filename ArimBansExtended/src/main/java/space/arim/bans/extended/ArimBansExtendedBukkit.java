/* 
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.extended.bukkit.CommandListener;

public class ArimBansExtendedBukkit extends JavaPlugin implements ArimBansExtendedPluginBase {

	private ArimBansExtended extended;
	
	@Override
	public void onEnable() {
		extended = new ArimBansExtended();
		loadCmds();
	}
	
	private void loadCmds() {
		CommandListener cmds = new CommandListener(this);
		for (String cmd : ArimBansExtended.commands()) {
			getServer().getPluginCommand(cmd).setExecutor(cmds);
		}
	}
	
	@Override
	public void onDisable() {
		close();
	}
	
	@Override
	public ArimBansExtended extension() {
		return extended;
	}
	
	@Override
	public void close() {
		ArimBansExtendedPluginBase.super.close();
	}

	@Override
	public Logger logger() {
		return getLogger();
	}

}
