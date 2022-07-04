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

package space.arim.libertybans.core.addon.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.env.PlatformHandle;
import space.arim.injector.Identifier;
import space.arim.injector.Injector;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.addon.AddonLoader;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddonsIT {

	private final Environment environment;

	private Path folder;
	private Injector injector;

	public AddonsIT(@Mock Environment environment) {
		this.environment = environment;
	}

	@BeforeEach
	public void setupInjector(@TempDir Path folder, @Mock PlatformHandle handle,
							  @Mock EnvEnforcer<?> envEnforcer, @Mock EnvUserResolver envUserResolver) {
		lenient().when(handle.createFuturesFactory()).thenReturn(new IndifferentFactoryOfTheFuture());
		when(handle.createEnhancedExecutor()).thenReturn(new SimplifiedEnhancedExecutor() {
			@Override
			public void execute(Runnable command) {
				ForkJoinPool.commonPool().execute(command);
			}
		});
		this.folder = folder;
		this.injector = new InjectorBuilder()
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(PlatformHandle.class, handle)
				.bindInstance(Environment.class, environment)
				.bindInstance(EnvEnforcer.class, envEnforcer)
				.bindInstance(EnvUserResolver.class, envUserResolver)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new CommandsModule(),
						new AddonITModule())
				.addBindModules(AddonLoader.loadAddonBindModules())
				.specification(SpecificationSupport.JAKARTA)
				.multiBindings(true)
				.build();
	}

	@Test
	public void initialize() {
		assertDoesNotThrow(() -> injector.request(BaseFoundation.class));
	}

	@Test
	public void startup() throws IOException {
		Files.createDirectories(folder.resolve("addons"));

		when(environment.createListeners()).thenReturn(Set.of());
		when(environment.createAliasCommand(any())).thenReturn(new PlatformListener() {
			@Override
			public void register() {}

			@Override
			public void unregister() {}
		});
		BaseFoundation foundation = injector.request(BaseFoundation.class);
		foundation.startup();
	}
}
