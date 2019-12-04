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
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import space.arim.bans.api.AsyncExecutor;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.util.ToolsUtil;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.async.AsyncMaster;
import space.arim.bans.internal.async.AsyncWrapper;
import space.arim.bans.internal.async.Async;
import space.arim.bans.internal.backend.punishment.Punishments;
import space.arim.bans.internal.backend.punishment.PunishmentsMaster;
import space.arim.bans.internal.backend.resolver.Resolver;
import space.arim.bans.internal.backend.resolver.ResolverMaster;
import space.arim.bans.internal.backend.subjects.SubjectsMaster;
import space.arim.bans.internal.backend.subjects.Subjects;
import space.arim.bans.internal.config.Config;
import space.arim.bans.internal.config.ConfigMaster;
import space.arim.bans.internal.frontend.commands.Commands;
import space.arim.bans.internal.frontend.commands.CommandsMaster;
import space.arim.bans.internal.frontend.format.Formats;
import space.arim.bans.internal.frontend.format.FormatsMaster;
import space.arim.bans.internal.sql.SqlMaster;
import space.arim.bans.internal.sql.Sql;

import space.arim.registry.UniversalRegistry;

public class ArimBansPlugin implements ArimBans {
	
	private final File folder;
	private Logger logger;
	private final Environment environment;
	private final ConfigMaster config;
	private final SqlMaster sql;
	private final PunishmentsMaster punishments;
	private final SubjectsMaster subjects;
	private final ResolverMaster resolver;
	private final CommandsMaster commands;
	private final FormatsMaster formats;
	private final AsyncMaster async;
	
	public ArimBansPlugin(File dataFolder, Environment environment) {
		this.folder = dataFolder;
		this.environment = environment;
		if (dataFolder.mkdirs()) {
			logger = Logger.getLogger(getName());
			logger.setParent(environment.logger());
			logger.setUseParentHandlers(false);
			String path = dataFolder.getPath() + File.separator + "logs" + File.separator + ToolsUtil.fileDateFormat() + File.separator;
			try {
				FileHandler verboseLog = new FileHandler(path + "verbose.log");
				FileHandler infoLog = new FileHandler(path + "info.log");
				FileHandler errorLog = new FileHandler(path + "error.log");
				verboseLog.setLevel(Level.ALL);
				infoLog.setLevel(Level.INFO);
				errorLog.setLevel(Level.WARNING);
				logger.addHandler(verboseLog);
				logger.addHandler(infoLog);
				logger.addHandler(errorLog);
			} catch (IOException ex) {
				shutdown("Logger initialisation in " + path + " failed!");
			}
		} else {
			shutdown("Directory creation of " + dataFolder.getPath() + " failed!");
		}
		config = new Config(this);
		sql = new Sql(this);
		punishments = new Punishments(this);
		subjects = new Subjects(this);
		resolver = new Resolver(this);
		commands = new Commands(this);
		formats = new Formats(this);
		if (UniversalRegistry.isProvidedFor(AsyncExecutor.class)) {
			async = new AsyncWrapper(UniversalRegistry.getRegistration(AsyncExecutor.class));
		} else {
			async = new Async(this);
			UniversalRegistry.register(AsyncExecutor.class, (AsyncExecutor) async); 
		}
		refresh(true);
		loadData();
		register();
	}
	
	private void shutdown(String message) {
		environment.shutdown(message);
		close();
	}

	@Override
	public File dataFolder() {
		return this.folder;
	}
	
	@Override
	public Environment environment() {
		return this.environment;
	}
	
	@Override
	public ConfigMaster config() {
		return config;
	}
	
	@Override
	public SqlMaster sql() {
		return sql;
	}
	
	@Override
	public PunishmentsMaster punishments() {
		return punishments;
	}

	@Override
	public SubjectsMaster subjects() {
		return subjects;
	}

	@Override
	public ResolverMaster resolver() {
		return resolver;
	}

	@Override
	public CommandsMaster commands() {
		return commands;
	}

	@Override
	public FormatsMaster formats() {
		return formats;
	}
	
	@Override
	public void log(Level level, String message) {
		if (logger != null) {
			logger.log(level, message);
			if (level.intValue() >= 800) {
				environment.logger().log(level, message);
			}
		} else {
			environment().logger().log(level, message);
		}
	}
	
	@Override
	public void logError(Exception ex) {
		if (logger != null) {
			environment().logger().warning("Encountered and caught an error: " + ex.getLocalizedMessage() + " \nPlease check the plugin's log for more information. Please create a Github issue at https://github.com/A248/ArimBans/issues to address this.");
			logger.log(Level.WARNING, "Encountered an error:", ex);
		} else {
			environment().logger().warning("Encountered and caught an error. \nThe plugin's log is inoperative, so the error will be printed to console. Please create a Github issue at https://github.com/A248/ArimBans/issues to address both problems.");
			ex.printStackTrace();
		}
	}
	
	@Override
	public int getNextAvailablePunishmentId() {
		return punishments.getNextAvailablePunishmentId();
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
		return Subject.fromUUID(subject);
	}
	
	@Override
	public Subject fromIpAddress(String address) throws IllegalArgumentException {
		return Subject.fromIP(address);
	}
	
	@Override
	public Subject parseSubject(String input) throws IllegalArgumentException {
		return subjects().parseSubject(input);
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
	public void reload() {
		refresh(false);
	}
	
	@Override
	public void reloadConfig() {
		refreshConfig(false);
	}
	
	@Override
	public void reloadMessages() {
		refreshMessages(false);
	}
	
	@Override
	public void sendMessage(Subject subject, String message) {
		subjects.sendMessage(subject, message);
	}

}