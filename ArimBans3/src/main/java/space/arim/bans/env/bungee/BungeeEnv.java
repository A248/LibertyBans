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
package space.arim.bans.env.bungee;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import space.arim.bans.ArimBans;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.util.Tools;
import space.arim.bans.env.Environment;

public class BungeeEnv implements Environment {

	private Plugin plugin;
	private final Set<EnvLibrary> libraries = loadLibraries();
	private ArimBans center;
	private final BungeeEnforcer enforcer;
	private final BungeeResolver resolver;
	private final BungeeListener listener;
	private final BungeeCommands commands;
	
	private boolean registered = false;
	private boolean json;
	
	public BungeeEnv(Plugin plugin) {
		this.plugin = plugin;
		this.enforcer = new BungeeEnforcer(this);
		this.resolver = new BungeeResolver(this);
		this.listener = new BungeeListener(this);
		this.commands = new BungeeCommands(this);
	}
	
	public void setCenter(ArimBans center) {
		this.center = center;
		if (!registered) {
			plugin.getProxy().getPluginManager().registerListener(plugin, listener);
			plugin.getProxy().getPluginManager().registerCommand(plugin, commands);
		}
	}
	
	void json(ProxiedPlayer target, String json) {
		target.sendMessage(Tools.parseJson(json));
	}
	
	@Override
	public void sendMessage(Subject subj, String jsonable) {
		if (json) {
			if (subj.getType().equals(SubjectType.PLAYER)) {
				ProxiedPlayer target = plugin.getProxy().getPlayer(subj.getUUID());
				if (target != null) {
					json(target, jsonable);
					return;
				}
				throw new InvalidSubjectException("Subject " + center.subjects().display(subj) + " is not online.");
			} else if (subj.getType().equals(SubjectType.CONSOLE)) {
				plugin.getProxy().getConsole().sendMessage(convert(Tools.encode(Tools.stripJson(jsonable))));
			} else if (subj.getType().equals(SubjectType.IP)) {
				for (ProxiedPlayer target : applicable(subj)) {
					json(target, jsonable);
				}
			} else {
				throw new InvalidSubjectException("Subject type is completely missing!");
			}
			return;
		} else if (!json) {
			if (subj.getType().equals(SubjectType.PLAYER)) {
				ProxiedPlayer target = plugin.getProxy().getPlayer(subj.getUUID());
				if (target != null) {
					target.sendMessage(convert(Tools.encode(jsonable)));
					return;
				}
				throw new InvalidSubjectException("Subject " + center.subjects().display(subj) + " is not online or does not have a valid UUID.");
			} else if (subj.getType().equals(SubjectType.CONSOLE)) {
				plugin.getProxy().getConsole().sendMessage(convert(Tools.encode(jsonable)));
			} else if (subj.getType().equals(SubjectType.IP)) {
				for (ProxiedPlayer target : applicable(subj)) {
					target.sendMessage(convert(Tools.encode(jsonable)));
				}
			} else {
				throw new InvalidSubjectException("Subject type is completely missing!");
			}
			return;
		}
		throw new InternalStateException("Json setting is neither true nor false!");
	}

	@Override
	public boolean hasPermission(Subject subject, String... permissions) {
		if (subject.getType().equals(SubjectType.CONSOLE)) {
			return true;
		} else if (subject.getType().equals(SubjectType.PLAYER)) {
			ProxiedPlayer target = plugin.getProxy().getPlayer(subject.getUUID());
			if (target != null) {
				for (String perm : permissions) {
					if (!target.hasPermission(perm)) {
						return false;
					}
				}
				return true;
			}
			throw new InvalidSubjectException("Subject " + center.subjects().display(subject) + " is not online.");
		} else if (subject.getType().equals(SubjectType.IP)) {
			throw new InvalidSubjectException("Cannot invoke Environment#hasPermission(Subject, Permission[]) for IP-based subjects");
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	public Set<ProxiedPlayer> applicable(Subject subject) {
		Set<ProxiedPlayer> applicable = new HashSet<ProxiedPlayer>();
		for (ProxiedPlayer check : plugin.getProxy().getPlayers()) {
			if (subject.getType().equals(SubjectType.PLAYER)) {
				if (subject.getUUID().equals(check.getUniqueId())) {
					applicable.add(check);
				}
			} else if (subject.getType().equals(SubjectType.IP)) {
				if (center.cache().hasIp(check.getUniqueId(), subject.getIP())) {
					applicable.add(check);
				}
			}
		}
		return applicable;
	}
	
	@Override
	public void runAsync(Runnable command) {
		plugin.getProxy().getScheduler().runAsync(plugin, command);
	}
	
	public Plugin plugin() {
		return plugin;
	}
	
	public ArimBans center() {
		return center;
	}
	
	@Override
	public BungeeEnforcer enforcer() {
		return enforcer;
	}
	
	@Override
	public BungeeResolver resolver() {
		return resolver;
	}
	
	@Override
	public Logger logger() {
		return plugin.getLogger();
	}

	@Override
	public String getName() {
		return plugin.getDescription().getName();
	}
	
	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthor();
	}
	
	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}
	
	@Override
	public boolean isLibrarySupported(EnvLibrary type) {
		return libraries.contains(type);
	}
	
	@Override
	public void refreshConfig() {
		json = center.config().getBoolean("formatting.use-json");
	}
	
	@Override
	public void close() {
		commands.close();
		listener.close();
		resolver.close();
		enforcer.close();
	}
	
	BaseComponent[] convert(String input) {
		return TextComponent.fromLegacyText(input);
	}

}
