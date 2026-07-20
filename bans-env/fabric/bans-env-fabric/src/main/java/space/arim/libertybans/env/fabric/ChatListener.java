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
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Singleton
public final class ChatListener extends FabricListener implements ServerMessageEvents.AllowChatMessage {

    private final Guardian guardian;
    private final ServerProvide serverProvide;

    private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

    @Inject
    public ChatListener(Guardian guardian, ServerProvide serverProvide) {
        this.guardian = guardian;
        this.serverProvide = serverProvide;
    }

    @Override
    void register0() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(Namespacing.ownIdentifier("chat.chat"), this);
    }

    private boolean combinedChatImpl(ServerPlayer player, @Nullable String command) {
        InetAddress address = FabricPlatformAccess.getAddress(player.connection.getRemoteAddress());
        CentralisedFuture<Component> futureMessage = guardian.checkChat(player.getUUID(), address, command);
        Component message;
        if (futureMessage.isDone()) {
            message = futureMessage.join();
            LOGGER.trace("Checked mute on {} and result is {}", Thread.currentThread(), message);
        } else if (!Thread.currentThread().equals(serverProvide.get().getRunningThread())) {
            LOGGER.warn("Cached mute unavailable for asynchronous chat/command event by player {}", player.getName());
            message = futureMessage.orTimeout(4L, TimeUnit.SECONDS).join();
        } else {
            LOGGER.error("Cached mute unavailable for asynchronous chat/command event by player {}", player.getName(), new IllegalStateException());
            message = null;
        }
        if (message == null) {
            return true;
        }
        player.sendMessage(message);
        return false;
    }

    boolean combinedChat(ServerPlayer player, @Nullable String command) {
        try {
            return combinedChatImpl(player, command);
        } catch (RuntimeException ex) {
            LOGGER.error("Unable to enforce mutes for chat or command occurrence.", ex);
            return true;
        }
    }

    @Override
    public boolean allowChatMessage(@NonNull PlayerChatMessage message, @NonNull ServerPlayer sender,
                                    ChatType.@NonNull Bound boundChatType) {
        return combinedChat(sender, null);
    }
}
