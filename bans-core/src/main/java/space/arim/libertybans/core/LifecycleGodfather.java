/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.env.EnvironmentManager;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.service.AsynchronicityManager;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.registry.Registration;
import space.arim.omnibus.registry.RegistryPriorities;
import space.arim.omnibus.util.ThisClass;

@Singleton
public class LifecycleGodfather extends AbstractBaseFoundation {

	private final AsynchronicityManager asyncManager;
	private final Configs configs;
	private final DatabaseManager databaseManager;
	private final UUIDManager uuidManager;
	private final MuteCache muteCache;
	private final TabCompletion tabCompletion;
	private final EnvironmentManager envManager;
	private final AddonCenter addonCenter;

	private final LibertyBans api;
	private Registration<LibertyBans> apiRegistration;

	private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public LifecycleGodfather(AsynchronicityManager asyncManager, Configs configs, DatabaseManager databaseManager,
							  UUIDManager uuidManager, MuteCache muteCache, TabCompletion tabCompletion,
							  EnvironmentManager envManager, AddonCenter addonCenter,
							  LibertyBans api) {
		this.asyncManager = asyncManager;
		this.configs = configs;
		this.databaseManager = databaseManager;
		this.uuidManager = uuidManager;
		this.muteCache = muteCache;
		this.tabCompletion = tabCompletion;
		this.envManager = envManager;
		this.addonCenter = addonCenter;

		this.api = api;
	}

	@Override
	void startup0() {
		asyncManager.startup();
		configs.startup();
		databaseManager.startup();
		uuidManager.startup();
		muteCache.startup();
		tabCompletion.startup();
		addonCenter.startup();
		envManager.startup();

		apiRegistration = api.getOmnibus().getRegistry()
				.register(LibertyBans.class, RegistryPriorities.LOWEST, api, "Reference impl");

		LOGGER.debug("Debug logging enabled");
		LOGGER.trace("Trace logging enabled");
	}

	@Override
	void restart0() {
		envManager.shutdown();
		asyncManager.restart();
		configs.restart();
		databaseManager.restart();
		uuidManager.restart();
		muteCache.restart();
		tabCompletion.restart();
		addonCenter.restart();
		envManager.startup();
	}

	@Override
	void shutdown0() {
		envManager.shutdown();
		addonCenter.shutdown();
		tabCompletion.shutdown();
		muteCache.shutdown();
		uuidManager.shutdown();
		configs.shutdown();
		asyncManager.shutdown();
		databaseManager.shutdown();

		api.getOmnibus().getRegistry().unregister(LibertyBans.class, apiRegistration);
	}

	@Override
	public Object platformAccess() {
		return envManager.platformAccess();
	}

}
