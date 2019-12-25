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
package space.arim.bans.internal.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import space.arim.bans.ArimBans;
import space.arim.bans.api.util.FilesUtil;

import space.arim.universal.util.logging.LogFormatter;

public class Logs implements LogsMaster {

	private final ArimBans center;
	
	private Logger logger;
	private FileHandler verboseLog;
	private FileHandler infoLog;
	private FileHandler errorLog;
	
	private int log_to_console_threshold = 800;
	private int log_directory_keep_alive = 20;
	
	public static final String PLEASE_CREATE_GITHUB_ISSUE_URL = "Please create a Github issue at https://github.com/A248/ArimBans/issues";
	
	public Logs(ArimBans center) {
		this.center = center;
	}
	
	@Override
	public boolean enabled() {
		return logger != null;
	}

	@Override
	public void log(Level level, String message) {
		if (logger != null) {
			logger.log(level, message);
		}
		if (level.intValue() > log_to_console_threshold) {
			center.environment().logger().log(level, message);
		}
	}
	
	@Override
	public void logBoth(Level level, String message) {
		if (logger != null) {
			logger.log(level, message);
		}
		center.environment().logger().log(level, message);
	}
	
	@Override
	public void logError(Exception ex) {
		if (logger != null) {
			center.environment().logger().warning("Encountered and caught an error: " + ex.getLocalizedMessage() + " \nPlease check the plugin's log for more information. " + PLEASE_CREATE_GITHUB_ISSUE_URL + " to address this.");
			logger.log(Level.WARNING, "Encountered and caught an error!", ex);
		} else {
			center.environment().logger().warning("Encountered and caught an error. \nThe plugin's log is inoperative, so the error will be printed to console. " + PLEASE_CREATE_GITHUB_ISSUE_URL + " to address both problems.");
			ex.printStackTrace();
		}
	}
	
	private void checkDeleteLogs() {
		long keepAlive = 86_400_000L * log_directory_keep_alive;
		long current = System.currentTimeMillis();
		File[] logDirs = (new File(center.dataFolder().getPath(), "logs")).listFiles();
		if (logDirs == null) {
			log(Level.WARNING, "Could not clean and delete old log folders");
			return;
		}
		for (File dir : logDirs) {
			if (dir.isDirectory() && current - dir.lastModified() > keepAlive) {
				if (dir.delete()) {
					log(Level.FINER, "Successfully cleaned & deleted old log folder " + dir.getPath());
				} else {
					log(Level.WARNING, "Could not clean & delete old log folder " + dir.getPath());
				}
			}
		}
	}
	
	@Override
	public void refreshConfig(boolean first) {
		log_to_console_threshold = center.config().getConfigInt("logs.log-to-console-threshold");
		log_directory_keep_alive = center.config().getConfigInt("logs.log-directory-keep-alive");
		if (first) {
			File path = FilesUtil.dateSuffixedFile(center.dataFolder(), "", "logs");
			try {
				if (!path.exists() && !path.mkdirs()) {
					center.environment().logger().warning("Failed to create logs directory!");
					return;
				}
				verboseLog = new FileHandler(path + File.separator + "verbose.log");
				infoLog = new FileHandler(path + File.separator + "info.log");
				errorLog = new FileHandler(path + File.separator + "error.log");
				Formatter universalFormatter = new LogFormatter();
				verboseLog.setFormatter(universalFormatter);
				infoLog.setFormatter(universalFormatter);
				errorLog.setFormatter(universalFormatter);
				verboseLog.setLevel(Level.ALL);
				infoLog.setLevel(Level.INFO);
				errorLog.setLevel(Level.WARNING);
				logger = Logger.getLogger(center.getName() + "-Core");
				logger.setParent(center.environment().logger());
				logger.setUseParentHandlers(false);
				logger.addHandler(verboseLog);
				logger.addHandler(infoLog);
				logger.addHandler(errorLog);
				center.environment().logger().log(Level.INFO, "Logging initialised in " + path);
			} catch (IOException ex) {
				center.environment().logger().log(Level.SEVERE, "Log initialisation failed!");
			}
			checkDeleteLogs();
		}
	}
	
	@Override
	public void close() {
		if (enabled()) {
			verboseLog.close();
			infoLog.close();
			errorLog.close();
		}
	}
	
}
