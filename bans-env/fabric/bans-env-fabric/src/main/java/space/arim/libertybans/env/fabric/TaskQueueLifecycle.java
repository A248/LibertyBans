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

import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.managedwaits.SimpleTaskQueue;
import space.arim.managedwaits.TaskQueue;

@Singleton
public final class TaskQueueLifecycle extends FabricListener implements ServerTickEvents.StartTick {

    final TaskQueue taskQueue = new SimpleTaskQueue();

    private volatile boolean cancel;

    @Override
    void register0() {
        ServerTickEvents.START_SERVER_TICK.register(Namespacing.ownIdentifier("tasks"), this);
    }

    void cancel() {
        cancel = true;
    }

    @Override
    public void onStartTick(@NonNull MinecraftServer server) {
        if (!cancel) {
            taskQueue.pollAndRunAll();
        }
    }
}