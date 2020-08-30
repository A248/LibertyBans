/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.nio.file.Path;
import java.util.Set;

import space.arim.omnibus.OmnibusProvider;

import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.plugin.UUIDVaultBungee;

import space.arim.api.env.BungeePlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.PlatformListener;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeEnv extends AbstractEnv {

	final LibertyBansCore core;
	final BungeePlatformHandle handle;
	
	private final BungeeEnforcer enforcer;
	
	public BungeeEnv(Plugin plugin, Path folder) {
		handle = new BungeePlatformHandle(plugin);
		core = new LibertyBansCore(OmnibusProvider.getOmnibus(), folder, this);

		enforcer = new BungeeEnforcer(this);
	}
	
	Plugin getPlugin() {
		return handle.getPlugin();
	}

	@Override
	public PlatformHandle getPlatformHandle() {
		return handle;
	}
	
	@Override
	public BungeeEnforcer getEnforcer() {
		return enforcer;
	}
	
	@Override
	protected void startup0() {
		if (UUIDVault.get() == null) {
			new UUIDVaultBungee(getPlugin()) {
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
		getPlugin().getLogger().info(message);
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of(
				new ConnectionListener(this),
				new ChatListener(this),
				new CommandHandler(this, Commands.BASE_COMMAND_NAME, false));
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		return new CommandHandler(this, command, true);
	}

}
