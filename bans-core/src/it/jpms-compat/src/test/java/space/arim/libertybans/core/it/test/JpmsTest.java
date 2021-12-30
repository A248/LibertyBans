/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.it.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.env.PlatformHandle;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.it.jpmscompat.JpmsLauncher;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JpmsTest {

	public @TempDir Path folder;

	private final PlatformHandle handle;
	private final Environment environment;
	private final EnvEnforcer<?> envEnforcer;
	private final EnvUserResolver envUserResolver;

	public JpmsTest(@Mock PlatformHandle handle, @Mock Environment environment,
					@Mock EnvEnforcer<?> envEnforcer, @Mock EnvUserResolver envUserResolver) {
		this.handle = handle;
		this.environment = environment;
		this.envEnforcer = envEnforcer;
		this.envUserResolver = envUserResolver;
	}

	private JpmsLauncher newLauncher() {
		return new JpmsLauncher(folder, handle, environment, envEnforcer, envUserResolver);
	}

	@BeforeEach
	public void setEnhancedExecutor() {
		when(handle.createEnhancedExecutor()).thenReturn(new SimplifiedEnhancedExecutor() {
			@Override
			public void execute(Runnable command) {
				ForkJoinPool.commonPool().execute(command);
			}
		});
	}

	@Test
	public void initialize() {
		BaseFoundation foundation = assertDoesNotThrow(() -> newLauncher().launch());
		assertNotNull(foundation);
	}

	@Test
	public void startupAndShutdown() {
		// This test is mostly intended to check that the configuration is exported to dazzleconf
		when(handle.createFuturesFactory()).thenReturn(new IndifferentFactoryOfTheFuture());
		when(environment.createListeners()).thenReturn(Set.of());
		when(environment.createAliasCommand(any())).thenReturn(new PlatformListener() {

			@Override
			public void register() { }

			@Override
			public void unregister() { }
		});

		BaseFoundation foundation = newLauncher().launch();
		assertDoesNotThrow(foundation::startup);
		assertDoesNotThrow(foundation::shutdown);
	}
}
