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

package space.arim.libertybans.env.fabric;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.PlatformPluginInfo;
import space.arim.api.env.concurrent.ClosableFactoryOfTheFuture;
import space.arim.managedwaits.DeadlockFreeFutureFactory;
import space.arim.managedwaits.LightSleepManagedWaitStrategy;
import space.arim.managedwaits.SimpleTaskQueue;
import space.arim.managedwaits.TaskQueue;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

import java.util.concurrent.ForkJoinPool;

public final class FabricPlatformHandle implements PlatformHandle {

    private final Thread mainThread;
    private final MinecraftServer server;
    private final ModContainer modContainer;

    @Inject
    public FabricPlatformHandle(@Named("mainThread") Thread mainThread, MinecraftServer server, ModContainer modContainer) {
        this.mainThread = mainThread;
        this.server = server;
        this.modContainer = modContainer;
    }

    @Override
    public FactoryOfTheFuture createFuturesFactory() {
        class FabricFactoryOfTheFuture extends DeadlockFreeFutureFactory implements ClosableFactoryOfTheFuture {

            private final RunTaskQueuePerTick runTaskQueuePerTick;

            FabricFactoryOfTheFuture(TaskQueue taskQueue, RunTaskQueuePerTick runTaskQueuePerTick) {
                super(taskQueue, new LightSleepManagedWaitStrategy());
                this.runTaskQueuePerTick = runTaskQueuePerTick;
            }

            @Override
            public boolean isPrimaryThread() {
                return mainThread == Thread.currentThread();
            }

            @Override
            public Thread getPrimaryThread() {
                return mainThread;
            }

            @Override
            public void close() {
                // Canceling now will allow existing tasks to finish -- see MinecraftServer implementation
                runTaskQueuePerTick.cancel();
            }
        }
        SimpleTaskQueue taskQueue = new SimpleTaskQueue();
        RunTaskQueuePerTick runTaskQueuePerTick = new RunTaskQueuePerTick(taskQueue, server);
        return new FabricFactoryOfTheFuture(taskQueue, runTaskQueuePerTick);
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
        return PlatformHandle.super.getPlatformVersion();
    }
}
