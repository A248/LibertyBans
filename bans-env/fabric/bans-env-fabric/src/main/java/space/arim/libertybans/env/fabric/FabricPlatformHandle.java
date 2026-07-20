/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.env.fabric;

import jakarta.inject.Inject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.PlatformPluginInfo;
import space.arim.api.env.concurrent.ClosableFactoryOfTheFuture;
import space.arim.managedwaits.DeadlockFreeFutureFactory;
import space.arim.managedwaits.LightSleepManagedWaitStrategy;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

import java.util.concurrent.ForkJoinPool;

public final class FabricPlatformHandle implements PlatformHandle {

    private final ServerProvide serverProvide;
    private final TaskQueueLifecycle taskQueueLifecycle;
    private final ModContainer modContainer;

    @Inject
    public FabricPlatformHandle(ServerProvide serverProvide, TaskQueueLifecycle taskQueueLifecycle,
                                ModContainer modContainer) {
        this.serverProvide = serverProvide;
        this.taskQueueLifecycle = taskQueueLifecycle;
        this.modContainer = modContainer;
    }

    @Override
    public FactoryOfTheFuture createFuturesFactory() {
        class FabricFactoryOfTheFuture extends DeadlockFreeFutureFactory implements ClosableFactoryOfTheFuture {

            FabricFactoryOfTheFuture() {
                super(taskQueueLifecycle.taskQueue, new LightSleepManagedWaitStrategy());
            }

            @Override
            public boolean isPrimaryThread() {
                return getPrimaryThread() == Thread.currentThread();
            }

            @Override
            public Thread getPrimaryThread() {
                return serverProvide.get().getRunningThread();
            }

            @Override
            public void close() {
                taskQueueLifecycle.cancel();
            }
        }
        return new FabricFactoryOfTheFuture();
    }

    @Override
    public EnhancedExecutor createEnhancedExecutor() {
        return new SimplifiedEnhancedExecutor() {
            @Override
            public void execute(Runnable command) {
                // Minecraft basically does the same thing
                ForkJoinPool.commonPool().execute(command);
            }
        };
    }

    @Override
    public PlatformPluginInfo getImplementingPluginInfo() {
        return new PlatformPluginInfo(modContainer, FabricLoader.getInstance());
    }

    @Override
    public String getPlatformVersion() {
        return serverProvide.get().getServerVersion();
    }
}
