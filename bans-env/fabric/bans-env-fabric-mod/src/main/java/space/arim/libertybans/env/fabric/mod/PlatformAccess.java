/*
 * LibertyBans-fabric
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans-fabric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibertyBans-fabric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibertyBans-fabric. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */

package space.arim.libertybans.env.fabric.mod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import space.arim.libertybans.bootstrap.BaseFoundation;

import java.net.SocketAddress;
import java.util.UUID;

public interface PlatformAccess {

    void installServer(MinecraftServer server);

	LiteralArgumentBuilder<CommandSourceStack> rootCommand();

    @Nullable Component checkConnection(UUID uuid, SocketAddress address, String name);

    boolean checkCommand(ServerPlayer player, String command);

    final class Holder {

        private static volatile PlatformAccess access;

        private Holder() {}

        public static @Nullable PlatformAccess getInstance() {
            return Holder.access;
        }

        static void install(BaseFoundation base) {
            access = access(base);
        }
    }

    static PlatformAccess access(BaseFoundation base) {
        return (PlatformAccess) base.platformAccess();
    }
}
