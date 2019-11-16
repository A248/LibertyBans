package space.arim.bans.env.bungee;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import space.arim.bans.ArimBans;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.Tools;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.env.Environment;
import space.arim.bans.env.Resolver;

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
				plugin.getProxy().getConsole().sendMessage(TextComponent.fromLegacyText(Tools.encode(Tools.stripJson(jsonable))));
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
					target.sendMessage(new TextComponent(TextComponent.fromLegacyText(Tools.encode(jsonable))));
					return;
				}
				throw new InvalidSubjectException("Subject " + center.subjects().display(subj) + " is not online or does not have a valid UUID.");
			} else if (subj.getType().equals(SubjectType.CONSOLE)) {
				plugin.getProxy().getConsole().sendMessage(TextComponent.fromLegacyText(Tools.encode(jsonable)));
			} else if (subj.getType().equals(SubjectType.IP)) {
				for (ProxiedPlayer target : applicable(subj)) {
					target.sendMessage(TextComponent.fromLegacyText(Tools.encode(jsonable)));
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
	public Resolver resolver() {
		return resolver;
	}
	
	public Plugin plugin() {
		return plugin;
	}
	
	public ArimBans center() {
		return center;
	}
	
	@Override
	public Logger logger() {
		return plugin.getLogger();
	}

	@Override
	public void close() throws Exception {
	}
	
	@Override
	public boolean isLibrarySupported(EnvLibrary type) {
		return libraries.contains(type);
	}

	@Override
	public void refreshConfig() {
		json = center.config().parseBoolean("formatting.use-json");
	}

	public BungeeEnforcer enforcer() {
		return enforcer;
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

}
