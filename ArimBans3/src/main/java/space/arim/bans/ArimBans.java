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

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.Configurable;
import space.arim.bans.internal.Component;
import space.arim.bans.internal.async.AsyncMaster;
import space.arim.bans.internal.async.Async;
import space.arim.bans.internal.backend.cache.Cache;
import space.arim.bans.internal.backend.cache.CacheMaster;
import space.arim.bans.internal.backend.punishment.Punishments;
import space.arim.bans.internal.backend.punishment.PunishmentsMaster;
import space.arim.bans.internal.backend.subjects.SubjectsMaster;
import space.arim.bans.internal.backend.subjects.Subjects;
import space.arim.bans.internal.config.Config;
import space.arim.bans.internal.config.ConfigMaster;
import space.arim.bans.internal.frontend.commands.Commands;
import space.arim.bans.internal.frontend.commands.CommandsMaster;
import space.arim.bans.internal.frontend.format.Formats;
import space.arim.bans.internal.frontend.format.FormatsMaster;
import space.arim.bans.internal.sql.SqlQuery;
import space.arim.registry.UniversalRegistry;
import space.arim.bans.internal.sql.SqlMaster;
import space.arim.bans.internal.sql.Sql;

public class ArimBans implements Configurable, ArimBansLibrary {
	
	private final File folder;
	private Logger logger;
	private final Environment environment;
	private final ConfigMaster config;
	private final SqlMaster sql;
	private final PunishmentsMaster punishments;
	private final SubjectsMaster subjects;
	private final CacheMaster cache;
	private final CommandsMaster commands;
	private final FormatsMaster formats;
	private final AsyncMaster async;
	
	@SuppressWarnings("unchecked")
	private <T extends Component> T load(Class<T> type, Component[] pool, Getter<T> getter) {
		for (Component component : pool) {
			if (type.isInstance(component)) {
				return (T) component;
			}
		}
		return getter.get();
	}
	
	public ArimBans(File dataFolder, Environment environment, Component...preloaded) {
		this.folder = dataFolder;
		this.environment = environment;
		if (dataFolder.mkdirs()) {
			logger = Logger.getLogger(getName());
			logger.setParent(environment.logger());
			logger.setUseParentHandlers(false);
			try {
				logger.addHandler(new FileHandler(dataFolder + File.separator + "information.log"));
			} catch (IOException ex) {
				environment.logger().warning("ArimBans: **Severe Error**\nLogger initialisation in " + dataFolder.getPath() + File.separator + "information.log failed!");
			}
		} else {
			environment().logger().warning("ArimBans: **Severe Error**\nDirectory creation of " + dataFolder.getPath() + " failed!");
		}
		config = load(ConfigMaster.class, preloaded, new Getter<ConfigMaster>() {
			@Override
			ConfigMaster get() {
				return new Config(ArimBans.this);
			}
		});
		sql = load(SqlMaster.class, preloaded, new Getter<SqlMaster>() {
			@Override
			SqlMaster get() {
				return new Sql(ArimBans.this);
			}
		});
		punishments = load(PunishmentsMaster.class, preloaded, new Getter<PunishmentsMaster>() {
			@Override
			PunishmentsMaster get() {
				return new Punishments(ArimBans.this);
			}
		});
		subjects = load(SubjectsMaster.class, preloaded, new Getter<SubjectsMaster>() {
			@Override
			SubjectsMaster get() {
				return new Subjects(ArimBans.this);
			}
		});
		cache = load(CacheMaster.class, preloaded, new Getter<CacheMaster>() {
			@Override
			CacheMaster get() {
				return new Cache(ArimBans.this);
			}
		});
		commands = load(CommandsMaster.class, preloaded, new Getter<CommandsMaster>() {
			@Override
			CommandsMaster get() {
				return new Commands(ArimBans.this);
			}
		});
		formats = load(FormatsMaster.class, preloaded, new Getter<FormatsMaster>() {
			@Override
			FormatsMaster get() {
				return new Formats(ArimBans.this);
			}
		});
		async = load(AsyncMaster.class, preloaded, new Getter<AsyncMaster>() {
			@Override
			AsyncMaster get() {
				return new Async();
			}
		});
		UniversalRegistry.register(PunishmentPlugin.class, this);
		loadData();
	}

	protected void loadData() {
		async(() -> {
			sql().executeQuery(new SqlQuery(SqlQuery.Query.CREATE_TABLE_CACHE.eval(sql().mode())), new SqlQuery(SqlQuery.Query.CREATE_TABLE_ACTIVE.eval(sql().mode())), new SqlQuery(SqlQuery.Query.CREATE_TABLE_HISTORY.eval(sql().mode())));
			ResultSet[] data = sql().selectQuery(new SqlQuery(SqlQuery.Query.SELECT_ALL_CACHED.eval(sql().mode())), new SqlQuery(SqlQuery.Query.SELECT_ALL_ACTIVE.eval(sql().mode())), new SqlQuery(SqlQuery.Query.SELECT_ALL_HISTORY.eval(sql().mode())));
			cache().loadAll(data[0]);
			punishments().loadActive(data[1]);
			punishments().loadHistory(data[2]);
		});
	}

	public File dataFolder() {
		return this.folder;
	}
	
	public Environment environment() {
		return this.environment;
	}
	
	public ConfigMaster config() {
		return config;
	}
	
	public SqlMaster sql() {
		return sql;
	}
	
	public PunishmentsMaster punishments() {
		return punishments;
	}

	public SubjectsMaster subjects() {
		return subjects;
	}

	public CacheMaster cache() {
		return cache;
	}

	public CommandsMaster commands() {
		return commands;
	}

	public FormatsMaster formats() {
		return formats;
	}
	
	public void log(String message) {
		if (logger != null) {
			logger.info(message);
		} else {
			environment().logger().info(message);
		}
	}
	
	public void logError(Exception ex) {
		if (logger != null) {
			environment().logger().warning("Encountered and caught an error: " + ex.getLocalizedMessage() + " \nPlease check the plugin's log for more information. Please create a Github issue to address this.");
			logger.log(Level.WARNING, "Encountered an error:", ex);
		} else {
			environment().logger().warning("Encountered and caught an error. \nNote that this plugin's log is inoperative, so the error will be printed to console. Please create a Github issue to address this.");
			ex.printStackTrace();
		}
	}
	
	@Override
	public void refreshConfig() {
		config.refreshConfig();
		async.refreshConfig();
		sql.refreshConfig();
		punishments.refreshConfig();
		punishments.refreshActive();
		commands.refreshConfig();
		cache.refreshConfig();
		formats.refreshConfig();
		async.refreshConfig();
	}
	
	@Override
	public void close() {
		try {
			config.close();
			async.close();
			sql.close();
			punishments.close();
			subjects.close();
			commands.close();
			cache.close();
			formats.close();
			async.close();
			//logger.close();
		} catch (Exception ex) {
			logError(ex);
		}
	}
	
	@Override
	public String getName() {
		return environment.getName();
	}
	
	@Override
	public String getAuthor() {
		return environment.getAuthor();
	}
	
	@Override
	public String getVersion() {
		return environment.getVersion();
	}
	
	@Override
	public byte getPriority() {
		return (byte) -32;
	}
	
	@Override
	public boolean isBanned(Subject subject) {
		return punishments().hasPunishment(subject, PunishmentType.BAN);
	}
	
	@Override
	public boolean isMuted(Subject subject) {
		return punishments().hasPunishment(subject, PunishmentType.MUTE);
	}
	
	@Override
	public Set<Punishment> getBanList() {
		return punishments().getAllPunishments(PunishmentType.BAN);
	}
	
	@Override
	public Set<Punishment> getMuteList() {
		return punishments().getAllPunishments(PunishmentType.MUTE);
	}
	
	@Override
	public Set<Punishment> getWarns(Subject subject) {
		return punishments().getPunishments(subject, PunishmentType.WARN);
	}
	
	@Override
	public Set<Punishment> getKicks(Subject subject) {
		return punishments().getPunishments(subject, PunishmentType.KICK);
	}
	
	@Override
	public Set<Punishment> getHistory(Subject subject) {
		return punishments().getHistory(subject);
	}
	
	@Override
	public void addPunishments(Punishment...punishments) throws ConflictingPunishmentException {
		punishments().addPunishments(punishments);
	}
	
	@Override
	public Subject fromUUID(UUID subject) {
		return subjects().parseSubject(subject);
	}
	
	@Override
	public Subject fromIpAddress(String address) throws IllegalArgumentException {
		return Subject.fromIP(address);
	}
	
	@Override
	public UUID uuidFromName(String name) throws PlayerNotFoundException {
		try {
			return cache().getUUID(name);
		} catch (MissingCacheException ex) {
			throw new PlayerNotFoundException(name, ex);
		}
	}
	
	@Override
	public UUID resolveName(String name) throws PlayerNotFoundException {
		return environment().resolver().uuidFromName(name);
	}

	@Override
	public String nameFromUUID(UUID uuid) throws PlayerNotFoundException {
		try {
			return cache().getName(uuid);
		} catch (MissingCacheException ex) {
			throw new PlayerNotFoundException(uuid, ex);
		}
	}
	
	@Override
	public String resolveUUID(UUID uuid) throws PlayerNotFoundException {
		return environment().resolver().nameFromUUID(uuid);
	}
	
	@Override
	public void simulateCommand(Subject subject, CommandType command, String[] args) {
		commands().execute(subject, command, args);
	}
	
	@Override
	public void async(Runnable command) {
		async.execute(command);
	}
	
	@Override
	public Logger getLogger() {
		return logger;
	}
	
	@Override
	public void sendMessage(Subject subject, String message) {
		environment.sendMessage(subject, message);
	}

}

abstract class Getter<T> {
	abstract T get();
}
