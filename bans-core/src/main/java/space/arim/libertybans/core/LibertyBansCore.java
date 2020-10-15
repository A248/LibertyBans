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
import java.util.concurrent.ThreadFactory;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.Formatter;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.Database;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.EnvironmentManager;
import space.arim.libertybans.core.punish.EnforcementCenter;
import space.arim.libertybans.core.punish.Scoper;
import space.arim.libertybans.core.selector.MuteCacher;
import space.arim.libertybans.core.selector.Selector;
import space.arim.libertybans.core.uuid.UUIDManager;

public class LibertyBansCore implements LibertyBans, Part {

	private final Path folder;
	private final Omnibus omnibus;
	private final AbstractEnv environment;
	
	private final Resources resources;
	private final Configs configs;
	private final DatabaseManager databaseManager;
	private final UUIDManager uuidMaster;
	private final EnvironmentManager envManager;
	
	private final EnforcementCenter enforcement;
	private final Selector selector;
	private final MuteCacher cacher;
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
		uuidMaster = new UUIDManager(this);
		envManager = new EnvironmentManager(this);

		selector = new Selector(this);
		cacher = new MuteCacher(this);
		enforcement = new EnforcementCenter(this);
		scoper = new Scoper();

		formatter = new Formatter(this);
		commands = new Commands(this);
	}
	
	@Override
	public void startup() {
		resources.startup();
		getConfigs().startup();
		databaseManager.startup();
		uuidMaster.startup();
		envManager.startup();
	}
	
	@Override
	public void restart() {
		envManager.shutdown();
		resources.restart();
		uuidMaster.restart();
		getConfigs().restart();
		databaseManager.restart();
		envManager.startup();
	}
	
	@Override
	public void shutdown() {
		envManager.shutdown();
		uuidMaster.shutdown();
		getConfigs().shutdown();
		databaseManager.shutdown();

		resources.shutdown();
	}
	
	public Path getFolder() {
		return folder;
	}
	
	public AbstractEnv getEnvironment() {
		return environment;
	}
	
	@Override
	public Omnibus getOmnibus() {
		return omnibus;
	}
	
	@Override
	public FactoryOfTheFuture getFuturesFactory() {
		return resources.getFuturesFactory();
	}
	
	public EnhancedExecutor getEnhancedExecutor() {
		return resources.getEnhancedExecutor();
	}
	
	public Configs getConfigs() {
		return configs;
	}
	
	public MainConfig getMainConfig() {
		return getConfigs().getMainConfig();
	}
	
	public MessagesConfig getMessagesConfig() {
		return getConfigs().getMessagesConfig();
	}
	
	@Override
	public Database getDatabase() {
		return databaseManager.getCurrentDatabase();
	}
	
	public DatabaseManager getDatabaseManager() {
		return databaseManager;
	}
	
	public UUIDManager getUUIDMaster() {
		return uuidMaster;
	}
	
	@Override
	public Selector getSelector() {
		return selector;
	}
	
	public MuteCacher getMuteCacher() {
		return cacher;
	}

	@Override
	public PunishmentDrafter getDrafter() {
		return enforcement;
	}
	
	public EnforcementCenter getEnforcementCenter() {
		return enforcement;
	}

	@Override
	public PunishmentRevoker getRevoker() {
		return enforcement.getRevoker();
	}

	@Override
	public Scoper getScopeManager() {
		return scoper;
	}
	
	@Override
	public Formatter getFormatter() {
		return formatter;
	}
	
	public Commands getCommands() {
		return commands;
	}
	
	public void postFuture(CentralisedFuture<?> future) {
		resources.postFuture(future);
	}
	
	public ThreadFactory newThreadFactory(String component) {
		return new ThreadFactoryImpl("LibertyBans-" + component + "-");
	}
	
}
