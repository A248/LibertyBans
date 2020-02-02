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

import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.extended.bungee.CommandSkeleton;
import space.arim.bans.extended.bungee.SignInterceptorProtocolize;

import space.arim.api.server.bungee.BungeeUtil;

public class ArimBansExtendedBungee extends Plugin implements ArimBansExtendedPluginBase {

	private ArimBansExtended extended;
	private Set<CommandSkeleton> cmds = new HashSet<CommandSkeleton>();
	private SignInterceptorProtocolize listener;
	
	@Override
	public void onEnable() {
		extended = new ArimBansExtended(getDataFolder(), getLogger());
		loadCmds();
		loadAntiSign();
	}
	
	private void loadCmds() {
		for (String cmd : ArimBansExtended.commands()) {
			CommandSkeleton skeleton = new CommandSkeleton(this, cmd);
			cmds.add(skeleton);
			getProxy().getPluginManager().registerCommand(this, skeleton);
		}
	}
	
	private void loadAntiSign() {
		if (extension().antiSignEnabled()) {
			listener = new SignInterceptorProtocolize(this);
			listener.register();
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
		getProxy().getPluginManager().unregisterCommands(this);
		ArimBansExtendedPluginBase.super.close();
	}
	
	public Iterable<String> getTabComplete(String[] args) {
		return BungeeUtil.getPlayerNameTabComplete(args, getProxy());
	}
	
}
