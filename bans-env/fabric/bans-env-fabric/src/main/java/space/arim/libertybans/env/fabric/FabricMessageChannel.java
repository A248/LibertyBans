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

import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.PluginMessageAsBytes;
import space.arim.libertybans.core.env.message.PluginMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public final class FabricMessageChannel extends FabricListener implements EnvMessageChannel<FabricMessageChannel.MessageHandler<?>> {

    private volatile List<MessageHandler<?>> handlers;

    @Override
    void register0() {
        StreamCodec<RegistryFriendlyByteBuf, BungeeMessage> codec = new StreamCodec<>() {
            @Override
            public BungeeMessage decode(RegistryFriendlyByteBuf input) {
                int readable = input.readableBytes();
                byte[] buffer = new byte[readable];
                input.readBytes(buffer);
                return new BungeeMessage(buffer);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, BungeeMessage value) {
                output.writeBytes(value.data());
            }
        };
        PayloadTypeRegistry.clientboundPlay().register(BungeeMessage.TYPE, codec);
        PayloadTypeRegistry.serverboundPlay().register(BungeeMessage.TYPE, codec);
        ServerPlayNetworking.registerGlobalReceiver(BungeeMessage.TYPE, new Receiver());
    }

    @Override
    public synchronized void installHandler(MessageHandler<?> handler) {
        List<MessageHandler<?>> updateHandlers = handlers;
        if (updateHandlers == null) {
            updateHandlers = new ArrayList<>();
        } else {
            updateHandlers = new ArrayList<>(updateHandlers);
        }
        updateHandlers.add(handler);
        handlers = updateHandlers;
    }

    @Override
    public synchronized void uninstallHandler(MessageHandler<?> handler) {
        List<MessageHandler<?>> updateHandlers = handlers;
        if (updateHandlers == null) {
            return;
        }
        updateHandlers = new ArrayList<>(updateHandlers);
        for (Iterator<MessageHandler<?>> it = updateHandlers.iterator(); it.hasNext(); ) {
            MessageHandler<?> current = it.next();
            if (current == handler) {
                it.remove();
                handlers = updateHandlers;
                return;
            }
        }
    }

    @Override
    public <R> MessageHandler<?> createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
        return new MessageHandler<>(acceptor, pluginMessage);
    }

    private final class Receiver implements ServerPlayNetworking.PlayPayloadHandler<BungeeMessage> {

        @Override
        public void receive(@NonNull BungeeMessage payload, ServerPlayNetworking.@NonNull Context context) {
            List<MessageHandler<?>> handlers = FabricMessageChannel.this.handlers;
            if (handlers == null) {
                return;
            }
            for (MessageHandler<?> handler : handlers) {
                handler.handle(payload.data());
            }
        }
    }

    public static final class MessageHandler<R> {

        private final Consumer<R> acceptor;
        private final PluginMessage<?, R> pluginMessage;

        private MessageHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
            this.acceptor = acceptor;
            this.pluginMessage = pluginMessage;
        }

        private void handle(byte[] data) {
            new PluginMessageAsBytes<>(pluginMessage).readBytes(data).ifPresent(acceptor);
        }
    }
}
