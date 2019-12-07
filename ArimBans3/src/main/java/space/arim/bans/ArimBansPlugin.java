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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import space.arim.bans.api.AsyncExecutor;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.util.ToolsUtil;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.async.AsyncMaster;
import space.arim.bans.internal.async.AsyncWrapper;
import space.arim.bans.internal.async.Async;
import space.arim.bans.internal.backend.punishment.Corresponder;
import space.arim.bans.internal.backend.punishment.Punishments;
import space.arim.bans.internal.backend.resolver.Resolver;
import space.arim.bans.internal.backend.subjects.Subjects;
import space.arim.bans.internal.config.Config;
import space.arim.bans.internal.frontend.commands.Commands;
import space.arim.bans.internal.frontend.format.Formats;
import space.arim.bans.internal.sql.Sql;

import space.arim.registry.UniversalRegistry;

public class ArimBansPlugin implements ArimBans {
	
	private final File folder;
	private Logger logger;
	private final Environment environment;
	private final Config config;
	private final Sql sql;
	private final Punishments punishments;
	private final Subjects subjects;
	private final Resolver resolver;
	private final Commands commands;
	private final Formats formats;
	private final Corresponder corresponder;
	private final AsyncMaster async;
	
	private static final String CREATE_GITHUB_ISSUE = "Please create a Github issue at https://github.com/A248/ArimBans/issues";
	private static final int LOG_TO_ENV_THRESHOLD = 800;
	
	public ArimBansPlugin(File folder, Environment environment) {
		this.folder = folder;
		this.environment = environment;
		if (folder.exists() || folder.mkdirs()) {
			logger = Logger.getLogger(getName());
			logger.setParent(environment.logger());
			logger.setUseParentHandlers(false);
			String path = folder.getPath() + File.separator + "logs" + File.separator + ToolsUtil.fileDateFormat() + File.separator;
			try {
				File dirPath = new File(path);
				if (!dirPath.exists() && !dirPath.mkdirs()) {
					shutdown("Directory creation of " + path + "failed!");
				}
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
			shutdown("Directory creation of " + folder.getPath() + " failed!");
		}
		config = new Config(this);
		sql = new Sql(this);
		punishments = new Punishments(this);
		subjects = new Subjects(this);
		resolver = new Resolver(this);
		commands = new Commands(this);
		formats = new Formats(this);
		corresponder = new Corresponder(this);
		if (UniversalRegistry.isProvidedFor(AsyncExecutor.class)) {
			async = new AsyncWrapper(UniversalRegistry.getRegistration(AsyncExecutor.class));
		} else {
			async = new Async(this);
			UniversalRegistry.register(AsyncExecutor.class, (AsyncExecutor) async); 
		}
		refresh(true);
		loadData();
		register();
		checkDeleteLogs();
	}
	
	private void shutdown(String message) {
		environment.shutdown(message);
		close();
		throw new InternalStateException("Shutting down...");
	}

	@Override
	public File dataFolder() {
		return folder;
	}
	
	@Override
	public Environment environment() {
		return environment;
	}
	
	@Override
	public Config config() {
		return config;
	}
	
	@Override
	public Sql sql() {
		return sql;
	}
	
	@Override
	public Punishments punishments() {
		return punishments;
	}

	@Override
	public Subjects subjects() {
		return subjects;
	}

	@Override
	public Resolver resolver() {
		return resolver;
	}

	@Override
	public Commands commands() {
		return commands;
	}

	@Override
	public Formats formats() {
		return formats;
	}
	
	@Override
	public Corresponder corresponder() {
		return corresponder;
	}
	
	@Override
	public void log(Level level, String message) {
		if (logger != null) {
			logger.log(level, message);
			if (level.intValue() >= LOG_TO_ENV_THRESHOLD) {
				environment.logger().log(level, message);
			}
		} else {
			environment().logger().log(level, message);
		}
	}
	
	@Override
	public void logError(Exception ex) {
		if (logger != null) {
			environment().logger().warning("Encountered and caught an error: " + ex.getLocalizedMessage() + " \nPlease check the plugin's log for more information. " + CREATE_GITHUB_ISSUE + " to address this.");
			logger.log(Level.WARNING, "Encountered an error:", ex);
		} else {
			environment().logger().warning("Encountered and caught an error. \nThe plugin's log is inoperative, so the error will be printed to console. " + CREATE_GITHUB_ISSUE + " to address both problems.");
			ex.printStackTrace();
		}
	}
	
	@Override
	public void async(Runnable command) {
		async.execute(command);
	}
	
	@Override
	public Logger getLogger() {
		return logger;
	}

}