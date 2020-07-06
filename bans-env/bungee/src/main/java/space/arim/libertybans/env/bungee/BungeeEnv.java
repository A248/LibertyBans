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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.plugin.bungee.UUIDVaultBungee;

import space.arim.api.chat.MessageParserUtil;
import space.arim.api.env.BungeeComponentParser;
import space.arim.api.env.convention.BungeePlatformConvention;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.OnlineTarget;

public class BungeeEnv extends AbstractEnv {

	final Plugin plugin;
	final LibertyBansCore core;
	
	private final Command command;
	private final ConnectionListener listener;
	
	public BungeeEnv(Plugin plugin, File folder) {
		this.plugin = plugin;
		core = new LibertyBansCore(new BungeePlatformConvention(plugin).getRegistry(), folder, this);
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
		if (UUIDVault.get() == null) {
			new UUIDVaultBungee(plugin).setInstancePassive();
		}
	}
	
	public BungeeEnv(Plugin plugin) {
		this(plugin, plugin.getDataFolder());
	}
	
	@Override
	public void sendToThoseWithPermission(String permission, String jsonable) {
		BaseComponent[] comps;
		BungeeComponentParser parser = new BungeeComponentParser();
		if (core.getFormatter().isUseJson()) {
			comps = parser.parseJson(jsonable);
		} else {
			comps = parser.colour(jsonable);
		}
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.hasPermission(permission)) {
				player.sendMessage(comps);
			}
		}
	}
	
	@Override
	public void kickByUUID(UUID uuid, String message) {
		ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
		if (player != null) {
			player.disconnect(TextComponent.fromLegacyText(message));
		}
	}

	@Override
	public CentralisedFuture<Set<OnlineTarget>> getOnlineTargets() {
		Set<OnlineTarget> result = new HashSet<>();
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			result.add(new BungeeOnlineTarget(player));
		}
		return core.getFuturesFactory().completedFuture(result);
	}
	
	void sendJson(CommandSender sender, String jsonable) {
		BungeeComponentParser parser = new BungeeComponentParser();
		if (sender instanceof ProxiedPlayer) {
			ProxiedPlayer player = (ProxiedPlayer) sender;
			if (core.getFormatter().isUseJson()) {
				player.sendMessage(parser.parseJson(jsonable));
			} else {
				player.sendMessage(parser.colour(jsonable));
			}
		} else {
			sender.sendMessage(parser.colour(new MessageParserUtil().removeRawJson(jsonable)));
		}
	}

	@Override
	protected void startup0() {
		core.startup();
		plugin.getProxy().getPluginManager().registerListener(plugin, listener);
		plugin.getProxy().getPluginManager().registerCommand(plugin, command);
	}

	@Override
	protected void shutdown0() {
		plugin.getProxy().getPluginManager().unregisterListener(listener);
		plugin.getProxy().getPluginManager().unregisterCommand(command);
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		plugin.getLogger().info(message);
	}

}
