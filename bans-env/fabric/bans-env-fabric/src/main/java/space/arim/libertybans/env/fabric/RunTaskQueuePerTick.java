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

import net.minecraft.server.MinecraftServer;
import space.arim.managedwaits.TaskQueue;

final class RunTaskQueuePerTick implements Runnable {

    private volatile boolean cancelled;
    private final TaskQueue taskQueue;
    private final MinecraftServer server;

    RunTaskQueuePerTick(TaskQueue taskQueue, MinecraftServer server) {
        this.taskQueue = taskQueue;
        this.server = server;
    }

    void cancel() {
        cancelled = true;
    }

    @Override
    public void run() {
        taskQueue.pollAndRunAll();

        if (!cancelled) {
            // Re-schedule for the next tick
            // This needs to be carefully tracked and updated with each Minecraft version
            server.send(server.createTask(this));
        }
    }
}
