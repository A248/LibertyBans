/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.BungeePlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.OnlineTarget;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public class BungeeEnv extends AbstractEnv {

	final LibertyBansCore core;
	final BungeePlatformHandle handle;
	
	private final Command command;
	private final ConnectionListener listener;
	
	public BungeeEnv(Plugin plugin, Path folder) {
		core = new LibertyBansCore(OmnibusProvider.getOmnibus(), folder, this);
		command = new Command("libertybans") {
			@Override
			public void execute(CommandSender sender, String[] args) {
				CmdSender iSender;
				if (sender instanceof ProxiedPlayer) {
					iSender = new PlayerCmdSender(BungeeEnv.this, (ProxiedPlayer) sender);
				} else {
					iSender = new ConsoleCmdSender(BungeeEnv.this, sender);
				}
				core.getCommands().execute(iSender, new ArrayCommandPackage("libertybans", args));
			}
		};
		listener = new ConnectionListener(this);
		handle = new BungeePlatformHandle(plugin);
	}
	
	public BungeeEnv(Plugin plugin) {
		this(plugin, plugin.getDataFolder().toPath());
	}
	
	Plugin getPlugin() {
		return handle.getPlugin();
	}
	
	@Override
	public Class<?> getPluginClass() {
		return getPlugin().getClass();
	}

	@Override
	public PlatformHandle getPlatformHandle() {
		return handle;
	}
	
	@Override
	public void sendToThoseWithPermission(String permission, SendableMessage message) {
		for (ProxiedPlayer player : getPlugin().getProxy().getPlayers()) {
			if (player.hasPermission(permission)) {
				handle.sendMessage(player, message);
			}
		}
	}
	
	@Override
	public void kickByUUID(UUID uuid, SendableMessage message) {
		ProxiedPlayer player = getPlugin().getProxy().getPlayer(uuid);
		if (player != null) {
			handle.disconnectUser(player, message);
		}
	}

	@Override
	public CentralisedFuture<Set<OnlineTarget>> getOnlineTargets() {
		Set<OnlineTarget> result = new HashSet<>();
		for (ProxiedPlayer player : getPlugin().getProxy().getPlayers()) {
			result.add(new BungeeOnlineTarget(this, player));
		}
		return core.getFuturesFactory().completedFuture(result);
	}

	@Override
	protected void startup0() {
		core.startup();
		PluginManager pm = getPlugin().getProxy().getPluginManager();
		pm.unregisterListener(listener);
		pm.unregisterCommand(command);
	}
	
	@Override
	protected void restart0() {
		core.restart();
	}

	@Override
	protected void shutdown0() {
		Plugin plugin = getPlugin();
		PluginManager pm = plugin.getProxy().getPluginManager();
		pm.registerListener(plugin, listener);
		pm.registerCommand(plugin, command);
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		getPlugin().getLogger().info(message);
	}

}
