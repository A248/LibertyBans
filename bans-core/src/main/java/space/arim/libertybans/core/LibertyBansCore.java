/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core;

import java.nio.file.Path;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.RunState;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.Database;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.uuid.UUIDMaster;

public class LibertyBansCore implements LibertyBans, Part {

	private final Path folder;
	private final Omnibus omnibus;
	private final AbstractEnv environment;
	
	private final Resources resources;
	private final Configs configs;
	private final DatabaseManager databaseManager;
	private final UUIDMaster uuidMaster;
	
	private final Selector selector;
	private final Enactor enactor;
	private final Enforcer enforcer;
	private final Scoper scoper;
	
	private final Formatter formatter;
	private final Commands commands;
	
	public LibertyBansCore(Omnibus omnibus,
			Path folder, AbstractEnv environment) {
		this.omnibus = omnibus;
		this.folder = folder;
		this.environment = environment;

		resources = new Resources(this);
		configs = new Configs(folder);
		databaseManager = new DatabaseManager(this);
		uuidMaster = new UUIDMaster(this);

		selector = new Selector(this);
		enactor = new Enactor(this);
		enforcer = new Enforcer(this);
		scoper = new Scoper();

		formatter = new Formatter(this);
		commands = new Commands(this);
	}
	
	@Override
	public void startup() {
		resources.restart();
		configs.startup();
		databaseManager.startup();
		uuidMaster.startup();
	}
	
	@Override
	public void restart() {
		resources.restart();
		uuidMaster.restart();
		configs.restart();
		databaseManager.restart();
	}
	
	@Override
	public void shutdown() {
		resources.restart();
		uuidMaster.shutdown();
		configs.shutdown();
		databaseManager.shutdown();
	}
	
	public Path getFolder() {
		return folder;
	}
	
	public AbstractEnv getEnvironment() {
		return environment;
	}
	
	@Override
	public RunState getRunState() {
		return environment.getRunState();
	}
	
	@Override
	public Omnibus getOmnibus() {
		return omnibus;
	}
	
	@Override
	public FactoryOfTheFuture getFuturesFactory() {
		return resources.getFuturesFactory();
	}
	
	public Resources getResources() {
		return resources;
	}
	
	public Configs getConfigs() {
		return configs;
	}
	
	@Override
	public Database getDatabase() {
		return databaseManager.getCurrentDatabase();
	}
	
	public DatabaseManager getDatabaseManager() {
		return databaseManager;
	}
	
	public UUIDMaster getUUIDMaster() {
		return uuidMaster;
	}
	
	@Override
	public Selector getSelector() {
		return selector;
	}

	@Override
	public Enactor getEnactor() {
		return enactor;
	}
	
	@Override
	public Enforcer getEnforcer() {
		return enforcer;
	}
	
	@Override
	public Scoper getScopeManager() {
		return scoper;
	}
	
	public Formatter getFormatter() {
		return formatter;
	}
	
	public Commands getCommands() {
		return commands;
	}
	
}
