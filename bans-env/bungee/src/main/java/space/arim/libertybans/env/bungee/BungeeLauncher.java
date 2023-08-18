/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.env.bungee;

import java.nio.file.Path;

import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;

import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import space.arim.libertybans.core.addon.AddonLoader;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

public final class BungeeLauncher implements PlatformLauncher {

	private final Plugin plugin;
	private final Path folder;
	private final Omnibus omnibus;

	public BungeeLauncher(Plugin plugin, Path folder) {
		this(plugin, folder, OmnibusProvider.getOmnibus());
	}

	public BungeeLauncher(Plugin plugin, Path folder, Omnibus omnibus) {
		this.plugin = plugin;
		this.folder = folder;
		this.omnibus = omnibus;
	}

	@Override
	public BaseFoundation launch() {
		return new InjectorBuilder()
				.bindInstance(Plugin.class, plugin)
				.bindInstance(ProxyServer.class, plugin.getProxy())
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(InstanceType.class, InstanceType.PROXY)
				.bindInstance(Omnibus.class, omnibus)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new BungeeBindModule())
				.addBindModules(AddonLoader.loadAddonBindModules())
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}

}
