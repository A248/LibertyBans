/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.util.ThisClass;

import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.plugin.UUIDVaultVelocity;

import space.arim.api.env.PlatformHandle;
import space.arim.api.env.VelocityPlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.PlatformListener;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

public class VelocityEnv extends AbstractEnv {

	final LibertyBansCore core;
	final VelocityPlatformHandle handle;
	
	private final VelocityEnforcer enforcer;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public VelocityEnv(Entry<PluginContainer, ProxyServer> pluginAndServer, Path folder) {
		PluginContainer plugin = pluginAndServer.getKey();
		ProxyServer server = pluginAndServer.getValue();

		core = new LibertyBansCore(OmnibusProvider.getOmnibus(), folder, this);
		handle = new VelocityPlatformHandle(plugin, server);

		enforcer = new VelocityEnforcer(this);
	}
	
	PluginContainer getPlugin() {
		return handle.getPlugin();
	}
	
	ProxyServer getServer() {
		return handle.getServer();
	}
	
	@Override
	public PlatformHandle getPlatformHandle() {
		return handle;
	}
	
	@Override
	public VelocityEnforcer getEnforcer() {
		return enforcer;
	}
	
	@Override
	protected void startup0() {
		if (UUIDVault.get() == null) {
			new UUIDVaultVelocity(getServer(), logger) {
				@Override
				protected boolean setInstancePassive() {
					return super.setInstancePassive();
				}
			}.setInstancePassive();
		}
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
	public Set<PlatformListener> createListeners() {
		return Set.of(
				new ConnectionListener(this),
				new ChatListener(this),
				new CommandListener(this),
				new CommandHandler(this, Commands.BASE_COMMAND_NAME, false));
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		return new CommandHandler(this, command, true);
	}
	
}
