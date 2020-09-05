/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.env;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.defaultimpl.DefaultOmnibus;
import space.arim.omnibus.util.ThisClass;

import space.arim.uuidvault.api.UUIDVault;

import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.LibertyBansCoreOverride;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.it.ConfigSpec;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackUUIDVault;

public class QuackEnv extends AbstractEnv implements CloseableResource {

	private final LibertyBansCore core;
	private final QuackPlatform quackPlatform;
	private final QuackHandle handle;
	private final QuackEnvEnforcer enforcer;
	private final UUIDVault uuidVault;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public QuackEnv(Path folder, ConfigSpec spec) {
		quackPlatform = new QuackPlatform();
		core = new LibertyBansCoreOverride(new DefaultOmnibus(), folder, this, spec);
		handle = new QuackHandle(quackPlatform);
		enforcer = new QuackEnvEnforcer(this, quackPlatform);
		uuidVault = new QuackUUIDVault(quackPlatform);
	}
	
	public LibertyBansCore core() {
		return core;
	}

	@Override
	public PlatformHandle getPlatformHandle() {
		return handle;
	}

	@Override
	public EnvEnforcer getEnforcer() {
		return enforcer;
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of();
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		return new PlatformListener() {

			@Override
			public void register() {
			}

			@Override
			public void unregister() {
			}
			
		};
	}

	@Override
	protected void startup0() {
		core.startup();
	}

	@Override
	protected void restart0() {
		core.restart();
	}

	@Override
	protected void shutdown0() {
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		logger.info(message);
	}
	
	@Override
	public UUIDVault getUUIDVault() {
		return uuidVault;
	}

	@Override
	public void close() throws Throwable {
		shutdown();
	}

}
