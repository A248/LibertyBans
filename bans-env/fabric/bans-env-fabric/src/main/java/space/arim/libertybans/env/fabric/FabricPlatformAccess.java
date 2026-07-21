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

package space.arim.libertybans.env.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import jakarta.inject.Inject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.env.fabric.mod.PlatformAccess;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class FabricPlatformAccess implements PlatformAccess {

    private final CommandHandler.Factory commandFactory;
    private final Guardian guardian;
    private final ServerAudiences serverAudiences;
    private final ChatListener chatListener;
    private final ServerProvide serverProvide;

    private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

    @Inject
    public FabricPlatformAccess(CommandHandler.Factory commandFactory, Guardian guardian,
                                ServerAudiences serverAudiences, ChatListener chatListener, ServerProvide serverProvide) {
        this.commandFactory = commandFactory;
        this.guardian = guardian;
        this.serverAudiences = serverAudiences;
        this.chatListener = chatListener;
        this.serverProvide = serverProvide;
    }

    static InetAddress getAddress(SocketAddress socketAddress) {
        if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress();
        } else {
            // Probably a player connected to an integrated server
            return InetAddress.getLoopbackAddress();
        }
    }

    @Override
    public void installServer(MinecraftServer server) {
        serverProvide.set(server);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> rootCommand() {
        return commandFactory.rootCommand();
    }

    @Override
    public @Nullable Component checkConnection(UUID uuid, SocketAddress address, String name) {
        LOGGER.debug("Checking connection by {} from socket {}", uuid, address);
        try {
            return checkConnection(uuid, getAddress(address), name);
        } catch (RuntimeException ex) {
            if (ex instanceof CompletionException && ex.getCause() instanceof TimeoutException) {
                LOGGER.warn("Timed out while trying to enforce connection by {} from socket {}", uuid, address);
            } else {
                LOGGER.error("Unable to check incoming connection", ex);
            }
            return null;
        }
    }

    private Component checkConnection(UUID uuid, InetAddress address, String name) {
        net.kyori.adventure.text.Component denial = guardian
                .executeAndCheckConnection(uuid, name, address)
                // Minecraft itself will time the player out after 30 seconds of logging in
                .orTimeout(25L, TimeUnit.SECONDS)
                .join();
        if (denial == null) {
            return null;
        }
        return serverAudiences.getServerAudiences().asNative(denial);
    }

    @Override
    public boolean checkCommand(ServerPlayer player, String command) {
        LOGGER.debug("Checking command {} by {}", command, player);
        return chatListener.combinedChat(player, command);
    }
}
