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

package space.arim.libertybans.env.standalone;

import org.slf4j.LoggerFactory;
import space.arim.injector.Identifier;
import space.arim.injector.Injector;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.addon.AddonLoader;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.nio.file.Path;

public final class StandaloneLauncher implements PlatformLauncher {

	private final Path folder;
	private final Omnibus omnibus;

	public StandaloneLauncher(/* Useless */ Void plugin, Path folder) {
		this(folder, OmnibusProvider.getOmnibus());
	}

	public StandaloneLauncher(Path folder, Omnibus omnibus) {
		this.folder = folder;
		this.omnibus = omnibus;
	}

	public Injector createInjector(ConsoleAudience consoleAudience) {
		return new InjectorBuilder()
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(InstanceType.class, InstanceType.STANDALONE)
				.bindInstance(Omnibus.class, omnibus)
				.bindInstance(ConsoleAudience.class, consoleAudience)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new StandaloneBindModule())
				.addBindModules(AddonLoader.loadAddonBindModules())
				.specification(SpecificationSupport.JAKARTA)
				.privateInjection(true)
				.multiBindings(true)
				.build();
	}

	@Override
	public BaseFoundation launch() {
		return createInjector(
				new ConsoleAudienceToLogger(LoggerFactory.getLogger(getClass()))
		).request(BaseFoundation.class);
	}

}
