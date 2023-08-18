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

package space.arim.libertybans.core.it.jpmscompat;

import space.arim.api.env.PlatformHandle;
import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.omnibus.DefaultOmnibus;
import space.arim.omnibus.Omnibus;

import java.nio.file.Path;

public class JpmsLauncher implements PlatformLauncher {

	private final Path folder;
	private final PlatformHandle handle;
	private final Environment environment;
	private final EnvEnforcer<?> envEnforcer;
	private final EnvUserResolver envUserResolver;

	public JpmsLauncher(Path folder, PlatformHandle handle, Environment environment,
						EnvEnforcer<?> envEnforcer, EnvUserResolver envUserResolver) {
		this.folder = folder;
		this.handle = handle;
		this.environment = environment;
		this.envEnforcer = envEnforcer;
		this.envUserResolver = envUserResolver;
	}

	@Override
	public BaseFoundation launch() {
		return new InjectorBuilder()
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(InstanceType.class, InstanceType.STANDALONE)
				.bindInstance(Omnibus.class, new DefaultOmnibus())
				.bindInstance(PlatformHandle.class, handle)
				.bindInstance(Environment.class, environment)
				.bindInstance(EnvEnforcer.class, envEnforcer)
				.bindInstance(EnvUserResolver.class, envUserResolver)
				.bindInstance(EnvServerNameDetection.class, (scopeManager) -> {})
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new JpmsBindModule())
				.multiBindings(true)
				.build()
				.request(BaseFoundation.class);
	}

}
