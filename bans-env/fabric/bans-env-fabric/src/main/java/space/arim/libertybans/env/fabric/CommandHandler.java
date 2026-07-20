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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import jakarta.inject.Inject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.commands.CommandSource;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AliasCommand;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.omnibus.util.ThisClass;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class CommandHandler implements Command<CommandSourceStack>, SuggestionProvider<CommandSourceStack>, AliasCommand {

    private final Factory commandFactory;
    private final String name;
    private final String aliasTarget;

    private static final AntiPersonnel ANTI_PERSONNEL = AntiPersonnel.load();
    private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

    private CommandHandler(Factory commandFactory, String name, @Nullable String aliasTarget) {
        this.commandFactory = commandFactory;
        this.name = name;
        this.aliasTarget = aliasTarget;
    }

    @Override
    public void register(RegisterOutcome outcome) {
        if (aliasTarget == null) {
            throw new UnsupportedOperationException();
        }
        if (ANTI_PERSONNEL == null) {
            outcome.deregistrationUnavailable();
            return;
        }
        if (ANTI_PERSONNEL.register(commandFactory.serverProvide.get(), buildCommand())) {
            outcome.success();
        } else {
            outcome.disappear();
        }
    }

    @Override
    public void unregister() {
        if (aliasTarget == null) {
            throw new UnsupportedOperationException();
        }
        if (ANTI_PERSONNEL == null) {
            return;
        }
        ANTI_PERSONNEL.deregister(commandFactory.serverProvide.get(), name);
    }

    public static final class Factory {

        private final InternalFormatter formatter;
        private final Interlocutor interlocutor;
        private final Commands commands;
        private final ServerProvide serverProvide;

        @Inject
        public Factory(InternalFormatter formatter, Interlocutor interlocutor, Commands commands, ServerProvide serverProvide) {
            this.formatter = formatter;
            this.interlocutor = interlocutor;
            this.commands = commands;
            this.serverProvide = serverProvide;
        }

        LiteralArgumentBuilder<CommandSourceStack> rootCommand() {
            return new CommandHandler(this, Commands.BASE_COMMAND_NAME, null).buildCommand();
        }

        CommandHandler dynamicCommand(String name, String aliasTarget) {
            Objects.requireNonNull(aliasTarget, "aliasTarget");
            return new CommandHandler(this, name, aliasTarget);
        }

        private CmdSender adaptSender(CommandSourceStack sender) {
            Operator operator;
            ServerPlayer player = sender.getPlayer();
            if (player == null) {
                operator = ConsoleOperator.INSTANCE;
            } else {
                operator = PlayerOperator.of(player.getUUID());
            }
            MinecraftServer server = serverProvide.get();
            return new FabricCmdSender(formatter, interlocutor, sender, operator, server);
        }
    }

    LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return net.minecraft.commands.Commands.literal(name)
                .requires(sender -> {
                    String command = Objects.requireNonNullElse(aliasTarget, name);
                    return commandFactory.commands.hasPermissionFor(
                            commandFactory.adaptSender(sender), command
                    );
                })
                .then(RequiredArgumentBuilder
                        .<CommandSourceStack, String>argument("value", StringArgumentType.greedyString())
                        .executes(this)
                        .suggests(this)
                )
                .executes(this);
    }

    private CommandSource adaptArgs(String input) {
        CommandSource args = new CommandSource.OfString(input);
        if (aliasTarget == null) {
            args.next();
        } else if (!aliasTarget.equals(name)) {
            args.next();
            args = new CommandSource.Prepended(aliasTarget, args);
        }
        return args;
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CmdSender sender = commandFactory.adaptSender(context.getSource());
        CommandSource args = adaptArgs(context.getInput());
        LOGGER.trace("Executing command by {} with arguments {}", sender, args);
        commandFactory.commands.execute(sender, args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                         SuggestionsBuilder builder) {
        CmdSender sender = commandFactory.adaptSender(context.getSource());
        String input = builder.getInput();
        {
            // Adjust the completion index; otherwise, the leading part of a partial argument is duplicated
            int lastSpaceIdx = input.lastIndexOf(' ');
            int offset = lastSpaceIdx == -1 ? 0 : lastSpaceIdx + 1;
            builder = builder.createOffset(offset);
        }
        if (input.isEmpty()) {
            LOGGER.warn("Expectations violated; empty input while computing suggestions");
            return builder.buildFuture();
        }
        CommandSource args = adaptArgs(input.substring(1));
        LOGGER.trace("Computing suggestions to send to {} based on args {}", sender, args);
        for (String suggestion : commandFactory.commands.suggest(sender, args)) {
            builder.suggest(suggestion);
        }
        return builder.buildFuture();
    }
}
