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

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.env.bukkit.BukkitEnv;

import space.arim.universal.registry.UniversalRegistry;
import space.arim.universal.util.AutoClosable;

import space.arim.api.uuid.UUIDResolver;

import net.milkbowl.vault.permission.Permission;

public class ArimBansBukkit extends JavaPlugin implements AutoClosable {
	
	private ArimBans center;
	private BukkitEnv environment;
	
	private Permission getPermissionPlugin() {
		if (getServer().getPluginManager().getPlugin("Vault") != null) {
            return null;
        }
		RegisteredServiceProvider<Permission> serviceProvider = getServer().getServicesManager().getRegistration(Permission.class);
		return serviceProvider == null ? null : serviceProvider.getProvider();
	}
	
	private void load() {
		environment = new BukkitEnv(this, getPermissionPlugin());
		center = new ArimBansPlugin(UniversalRegistry.get(), getDataFolder(), environment);
		center.start();
		environment.loadFor(center);
		center.getRegistry().register(PunishmentPlugin.class, center);
		center.getRegistry().register(UUIDResolver.class, center.resolver());
	}
	
	@Override
	public void close() {
		center.close();
		environment.close();
	}
	
	@Override
	public void onEnable() {
		load();
	}

	@Override
	public void onDisable() {
		close();
	}
}
