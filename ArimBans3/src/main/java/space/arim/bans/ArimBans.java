package space.arim.bans;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.Replaceable;
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
import space.arim.bans.internal.sql.SqlMaster;
import space.arim.bans.internal.sql.Sql;

public class ArimBans implements AutoCloseable {
	
	private File folder;
	private PrintStream logger;
	private SimpleDateFormat dateFormatter;
	private Environment environment;
	private ConfigMaster config;
	private PunishmentsMaster manager;
	private SqlMaster sql;
	private SubjectsMaster subjects;
	private CacheMaster cache;
	private CommandsMaster commands;
	private FormatsMaster formatter;
	private AsyncMaster threads;
	
	public ArimBans(File dataFolder, Environment environment, Replaceable...preloaded) {
		this.folder = dataFolder;
		this.environment = environment;
		if (dataFolder.mkdirs()) {
			loadWriter(dataFolder.getPath() + File.separator + "info.log");
		} else {
			environment().logger().warning("The logger could not be loaded! Reason: Directory creation failed.");
		}
		for (Replaceable obj : preloaded) {
			if (obj instanceof ConfigMaster) {
				this.config = (ConfigMaster) obj;
			} else if (obj instanceof PunishmentsMaster) {
				this.manager = (PunishmentsMaster) obj;
			} else if (obj instanceof SqlMaster) {
				this.sql = (SqlMaster) obj;
			} else if (obj instanceof SubjectsMaster) {
				this.subjects = (SubjectsMaster) obj;
			} else if (obj instanceof FormatsMaster) {
				this.formatter = new Formats(this);
			} else if (obj instanceof AsyncMaster) {
				this.threads = new Async();
			} else if (obj instanceof CommandsMaster) {
				this.commands = (CommandsMaster) obj;
			} else if (obj instanceof CacheMaster) {
				this.cache = (CacheMaster) obj;
			}
		}
		try {
			if (this.config == null) {
				this.config = new Config(this);
			}
			if (this.manager == null) {
				this.manager = new Punishments(this);
			}
			if (this.sql == null) {
				this.sql = new Sql(this);
			}
			if (this.subjects == null) {
				this.subjects = new Subjects(this);
			}
			if (this.formatter == null) {
				this.formatter = new Formats(this);
			}
			if (this.threads == null) {
				this.threads = new Async();
			}
			if (this.commands == null) {
				this.commands = new Commands(this);
			}
			if (this.cache == null) {
				this.cache = new Cache(this);
			}
		} catch (Exception ex) {
			throw new InternalStateException("Encountered an error while loading!", ex);
		}
		loadData();
	}

	private static boolean checkFile(File file) throws IOException {
		if (file.exists() && file.canRead() && file.canWrite()) {
			return true;
		} else if (file.exists()) {
			file.delete();
		}
		if (!file.getParentFile().mkdirs()) {
			return false;
		}
		if (!file.createNewFile()) {
			return false;
		}
		return true;
	}
	
	protected void loadWriter(String source) {
		try {
			File file = new File(source);
			if (checkFile(file)) {
				dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				logger = new PrintStream(file);
			}
		} catch (Exception ex) {
			environment().logger().warning("The logger could not be loaded! Reason: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}

	protected void loadData() {
		async().execute(() -> {
			sql().executeQuery(new SqlQuery(SqlQuery.Query.CREATE_TABLE_CACHE.eval(sql().mode())), new SqlQuery(SqlQuery.Query.CREATE_TABLE_ACTIVE.eval(sql().mode())), new SqlQuery(SqlQuery.Query.CREATE_TABLE_HISTORY.eval(sql().mode())));
			ResultSet[] data = sql().selectQuery(new SqlQuery(SqlQuery.Query.SELECT_ALL_CACHED.eval(sql().mode())), new SqlQuery(SqlQuery.Query.SELECT_ALL_ACTIVE.eval(sql().mode())), new SqlQuery(SqlQuery.Query.SELECT_ALL_HISTORY.eval(sql().mode())));
			cache().loadAll(data[0]);
			manager().loadActive(data[1]);
			manager().loadHistory(data[2]);
		});
	}

	public PunishmentsMaster manager() {
		return manager;
	}

	public SqlMaster sql() {
		return sql;
	}

	public ConfigMaster config() {
		return config;
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

	public FormatsMaster formatter() {
		return formatter;
	}

	public AsyncMaster async() {
		return threads;
	}

	public File dataFolder() {
		return this.folder;
	}
	
	public Environment environment() {
		return this.environment;
	}
	
	public void log(String message) {
		if (logger != null && dateFormatter != null) {
			logger.append("[" + dateFormatter.format(new Date()) + "] " + message + "\n");
		} else {
			environment().logger().info(message);
		}
	}
	
	public void logError(Exception ex) {
		if (logger != null) {
			environment().logger().warning("Encountered and caught an error: " + ex.getLocalizedMessage() + " \nPlease check the plugin's log for more information. Please create a Github issue to address this.");
			ex.printStackTrace(logger);
		} else {
			environment().logger().warning("Encountered and caught an error. \nNote that this plugin's log is inoperative, so the error will be printed to console. Please create a Github issue to address this.");
			ex.printStackTrace();
		}
	}
	
	public void refreshConfig() {
		config.refreshConfig();
		threads.refreshConfig();
		sql.refreshConfig();
		manager.refreshConfig();
	}
	
	@Override
	public void close() {
		try {
			config.close();
			threads.shutdown();
			sql.close();
			manager.close();
			subjects.close();
			commands.close();
			cache.close();
			formatter.close();
			threads.close();
			logger.close();
		} catch (Exception ex) {
			logError(ex);
		}
	}

}
