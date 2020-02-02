/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.sponge;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.user.UserStorageService;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.env.Environment;

import space.arim.api.config.YmlLoader;
import space.arim.api.server.sponge.SpongeUtil;

public class SpongeEnv implements Environment, YmlLoader {

	private final PluginContainer plugin;
	private final Logger logger;
	private final String name;
	private final String author;
	private final String version;
	private ArimBans center;
	private final SpongeEnforcer enforcer;
	private final SpongeListener listener;
	private final SpongeCommands commands;
	
	private boolean registered = false;
	
	public SpongeEnv(PluginContainer plugin) {
		this.plugin = plugin;
		Map<String, Object> spongeInfo = loadResource("sponge.yml");
		name = Objects.requireNonNull(spongeInfo.get("name"), "sponge.yml invalid!").toString();
		author = Objects.requireNonNull(spongeInfo.get("author"), "sponge.yml invalid!").toString();
		version = Objects.requireNonNull(spongeInfo.get("version"), "sponge.yml invalid!").toString();
		logger = Logger.getLogger(name);
		logger.setParent(Logger.getLogger(""));
		enforcer = new SpongeEnforcer(this);
		listener = new SpongeListener(this);
		commands = new SpongeCommands(this);
	}
	
	@Override
	public void loadFor(ArimBans center) {
		this.center = center;
		if (!registered) {
			Sponge.getEventManager().registerListeners(plugin.getInstance().get(), listener);
			Sponge.getCommandManager().register(plugin.getInstance().get(), commands, "arimbans");
			registered = true;
		}
	}
	
	@Override
	public boolean isOnlineMode() {
		return server().getOnlineMode();
	}
	
	void sendMessage(Player target, String jsonable, boolean useJson) {
		if (useJson) {
			target.sendMessage(SpongeUtil.parseJson(jsonable));
		} else {
			target.sendMessage(SpongeUtil.colour(jsonable));
		}
	}
	
	void sendConsoleMessage(String jsonable) {
		server().getConsole().sendMessage(SpongeUtil.colour(SpongeUtil.stripJson(jsonable)));
	}
	
	@Override
	public boolean isOnline(Subject subject) {
		switch (subject.getType()) {
		case PLAYER:
			return server().getPlayer(subject.getUUID()).isPresent();
		case IP:
			return server().getOnlinePlayers().stream().anyMatch((player) -> center.resolver().hasIp(player.getUniqueId(), subject.getIP()));
		default:
			return subject.getType().equals(SubjectType.CONSOLE);
		}
	}
	
	@Override
	public void sendMessage(Subject subj, String jsonable, boolean useJson) {
		if (subj.getType().equals(SubjectType.PLAYER)) {
			server().getPlayer(subj.getUUID()).ifPresent((target) -> sendMessage(target, jsonable, useJson));
		} else if (subj.getType().equals(SubjectType.CONSOLE)) {
			sendConsoleMessage(jsonable);
		} else if (subj.getType().equals(SubjectType.IP)) {
			applicable(subj).forEach((target) -> sendMessage(target, jsonable, useJson));
		}
	}
	
	@Override
	public void sendMessage(String permission, String jsonable, boolean useJson) {
		server().getOnlinePlayers().forEach((player) -> {
			if (player.hasPermission(permission)) {
				sendMessage(player, jsonable, useJson);
			}
		});
	}
	
	private boolean checkOfflinePlayerPermission(UUID uuid, String permission) {
		Optional<Player> player = server().getPlayer(uuid);
		if (player.isPresent()) {
			return player.get().hasPermission(permission);
		}
		Optional<UserStorageService> storage = Sponge.getServiceManager().provide(UserStorageService.class);
		if (storage.isPresent()) {
			Optional<User> offlinePlayer = storage.get().get(uuid);
			if (offlinePlayer.isPresent()) {
				return offlinePlayer.get().hasPermission(permission);
			}
		}
		return false;
	}
	
	@Override
	public boolean hasPermission(Subject subject, String permission, boolean opPerms) {
		if (subject.getType().equals(SubjectType.CONSOLE)) {
			return true;
		} else if (subject.getType().equals(SubjectType.PLAYER)) {
			return checkOfflinePlayerPermission(subject.getUUID(), permission);
		} else if (subject.getType().equals(SubjectType.IP)) {
			return center.resolver().getPlayers(subject.getIP()).stream().anyMatch((uuid) -> checkOfflinePlayerPermission(uuid, permission));
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	Set<Player> applicable(Subject subject) {
		switch (subject.getType()) {
		case PLAYER:
			Set<Player> applicable1 = new HashSet<Player>();
			server().getOnlinePlayers().forEach((check) -> {
				if (subject.getUUID().equals(check.getUniqueId())) {
					applicable1.add(check);
				}
			});
			return applicable1;
		case IP:
			Set<Player> applicable2 = new HashSet<Player>();
			server().getOnlinePlayers().forEach((check) -> {
				if (center.resolver().hasIp(check.getUniqueId(), subject.getIP())) {
					applicable2.add(check);
				}
			});
			return applicable2;
		default:
			return Collections.emptySet();
		}
	}
	
	public Server server() {
		return Sponge.getServer();
	}
	
	public ArimBans center() {
		return center;
	}
	
	@Override
	public void enforce(Punishment punishment, boolean useJson) {
		enforcer().enforce(punishment, useJson);
	}
	
	SpongeEnforcer enforcer() {
		return enforcer;
	}
	
	@Override
	public UUID uuidFromName(String name) {
		return server().getPlayer(name).map((player) -> player.getUniqueId()).orElse(null);
	}
	
	@Override
	public String nameFromUUID(UUID uuid) {
		return server().getPlayer(uuid).map((player) -> player.getName()).orElse(null);
	}
	
	@Override
	public Logger logger() {
		return logger;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getAuthor() {
		return author;
	}
	
	@Override
	public String getVersion() {
		return version;
	}

}
