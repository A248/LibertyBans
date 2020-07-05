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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import space.arim.universal.util.ThisClass;

import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.plugin.bungee.UUIDVaultBungee;

import space.arim.api.chat.MessageParserUtil;
import space.arim.api.env.BungeeComponentParser;
import space.arim.api.env.convention.BungeePlatformConvention;

import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.CmdSender;

public class BungeeEnv extends AbstractEnv {

	final Plugin plugin;
	final LibertyBansCore core;
	
	private final Command command;
	private final ConnectionListener listener;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
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
		if (useJson()) {
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
	public void enforcePunishment(Punishment punishment) {
		logger.debug("Enforcing {}", punishment);
		String message = core.getConfigs().getPunishmentMessage(punishment);
		PunishmentType type = punishment.getType();
		switch (type) {
		case BAN:
		case KICK:
			ProxiedPlayer player = plugin.getProxy().getPlayer(((PlayerVictim) punishment.getVictim()).getUUID());
			if (player != null) {
				player.disconnect(TextComponent.fromLegacyText(message));
			}
			break;
		case MUTE:
			break;
		case WARN:
			break;
		default:
			throw new IllegalStateException("Unknown punishment type " + type);
		}
	}
	
	private boolean useJson() {
		return core.getConfigs().getMessages().getBoolean("json.enable");
	}
	
	void sendJson(CommandSender sender, String jsonable) {
		BungeeComponentParser parser = new BungeeComponentParser();
		if (sender instanceof ProxiedPlayer) {
			ProxiedPlayer player = (ProxiedPlayer) sender;
			if (useJson()) {
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
