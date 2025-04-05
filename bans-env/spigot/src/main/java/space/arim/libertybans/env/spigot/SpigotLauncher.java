/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.env.spigot;

import java.nio.file.Path;

import org.bukkit.plugin.Plugin;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.PlatformId;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;

import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.libertybans.core.addon.AddonLoader;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

public final class SpigotLauncher implements PlatformLauncher {

	private final Payload<JavaPlugin> payload;
	private final Omnibus omnibus;

	public SpigotLauncher(Payload<JavaPlugin> payload) {
		this(payload, OmnibusProvider.getOmnibus());
	}

	public SpigotLauncher(Payload<JavaPlugin> payload, Omnibus omnibus) {
		this.payload = payload;
		this.omnibus = omnibus;
	}

	@Override
	public BaseFoundation launch() {
		JavaPlugin plugin = payload.plugin();
		return new InjectorBuilder()
				.bindInstance(JavaPlugin.class, plugin)
				.bindInstance(Plugin.class, plugin)
				.bindInstance(Server.class, plugin.getServer())
				.bindInstance(PlatformId.class, payload.platformId())
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), payload.pluginFolder())
				.bindInstance(InstanceType.class, InstanceType.GAME_SERVER)
				.bindInstance(Omnibus.class, omnibus)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new SpigotBindModule())
				.addBindModules(AddonLoader.loadAddonBindModules())
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}

}
