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
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.AliasCommand;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.env.fabric.mod.PlatformAccess;

import java.util.Set;

public final class FabricEnv implements Environment {

    private final Configs configs;
    private final ServerAudiences serverAudiences;
    private final TaskQueueLifecycle taskQueueLifecycle;
    private final FabricMessageChannel messageChannel;
    private final ChatListener chatListener;
    private final JoinListener joinListener;
    private final CommandHandler.Factory commandFactory;
    private final ServerProvide serverProvide;
    private final PlatformAccess platformAccess;

    @Inject
    public FabricEnv(Configs configs, ServerAudiences serverAudiences, TaskQueueLifecycle taskQueueLifecycle,
                     FabricMessageChannel messageChannel, ChatListener chatListener, JoinListener joinListener,
                     CommandHandler.Factory commandFactory, ServerProvide serverProvide, PlatformAccess platformAccess) {
        this.configs = configs;
        this.serverAudiences = serverAudiences;
        this.taskQueueLifecycle = taskQueueLifecycle;
        this.messageChannel = messageChannel;
        this.chatListener = chatListener;
        this.joinListener = joinListener;
        this.commandFactory = commandFactory;
        this.serverProvide = serverProvide;
        this.platformAccess = platformAccess;
    }

    @Override
    public Set<PlatformListener> createListeners(AliasCommand.RegisterOutcome registerRootCommand) {
        return Set.of(serverAudiences, taskQueueLifecycle, messageChannel, chatListener, joinListener);
    }

    @Override
    public AliasCommand createAliasCommand(String alias, String target) {
        if (!configs.getMainConfig().platforms().fabric().registerCommandsDynamically()) {
            return null;
        }
        return commandFactory.dynamicCommand(alias, target);
    }

    @Override
    public void refreshServerCommands() {
        MinecraftServer server = serverProvide.get();
        Commands commands = server.getCommands();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            commands.sendCommands(player);
        }
    }

    @Override
    public Object platformAccess() {
        return platformAccess;
    }
}
