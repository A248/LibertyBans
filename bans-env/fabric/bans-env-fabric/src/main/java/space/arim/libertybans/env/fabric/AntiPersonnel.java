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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.omnibus.util.ThisClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;

final class AntiPersonnel {

    private final MethodHandle childrenField;
    private final MethodHandle literalsField;
    private final MethodHandle argumentsField;

    private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

    private AntiPersonnel(MethodHandle childrenField, MethodHandle literalsField, MethodHandle argumentsField) {
        this.childrenField = childrenField;
        this.literalsField = literalsField;
        this.argumentsField = argumentsField;
    }

    static AntiPersonnel load() {
        try {
            return load0();
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            String msg = "Failed to load dynamic command registration/deregistration device";
            if (LOGGER.isDebugEnabled()) {
                LOGGER.warn(msg, ex);
            } else {
                LOGGER.warn(msg);
            }
            return null;
        }
    }

    private static AntiPersonnel load0() throws IllegalAccessException, NoSuchFieldException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        lookup = MethodHandles.privateLookupIn(CommandNode.class, lookup);
        MethodHandle childrenField = lookup.findGetter(CommandNode.class, "children", Map.class);
        MethodHandle literalsField = lookup.findGetter(CommandNode.class, "literals", Map.class);
        MethodHandle argumentsField = lookup.findGetter(CommandNode.class, "arguments", Map.class);
        return new AntiPersonnel(childrenField, literalsField, argumentsField);
    }

    private record NodeFields<S>(Map<String, CommandNode<S>> children, Map<String, LiteralCommandNode<S>> literals,
                                 Map<String, ArgumentCommandNode<S, ?>> arguments) {

        void deregister(String name) {
            LOGGER.debug("Forcibly deregistering {} via reflection", name);
            children.remove(name);
            literals.remove(name);
            arguments.remove(name);
        }
    }

    @SuppressWarnings("unchecked")
    private <S> NodeFields<S> loadFields(CommandNode<S> node) {
        Map<String, CommandNode<S>> children;
        Map<String, LiteralCommandNode<S>> literals;
        Map<String, ArgumentCommandNode<S, ?>> arguments;
        try {
            children = (Map<String, CommandNode<S>>) childrenField.invokeExact(node);
            literals = (Map<String, LiteralCommandNode<S>>) literalsField.invokeExact(node);
            arguments = (Map<String, ArgumentCommandNode<S, ?>>) argumentsField.invokeExact(node);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
        return new NodeFields<>(children, literals, arguments);
    }

    boolean register(MinecraftServer server, LiteralArgumentBuilder<CommandSourceStack> command) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        String name = command.getLiteral();
        NodeFields<CommandSourceStack> fields = loadFields(dispatcher.getRoot());
        if (fields.children.containsKey(name)) {
            // Forcibly deregister existing commands
            fields.deregister(name);
        }
        dispatcher.register(command);
        LOGGER.debug("Registered new command for {}", name);
        return fields.children.containsKey(name) && fields.literals.containsKey(name);
    }

    void deregister(MinecraftServer server, String name) {
        loadFields(server.getCommands().getDispatcher().getRoot()).deregister(name);
    }
}
