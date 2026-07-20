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

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.player.Player;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;
import space.arim.libertybans.core.env.Interlocutor;

import java.util.stream.Stream;

final class FabricCmdSender extends AbstractCmdSender<CommandSourceStack> {

    private final MinecraftServer server;

    FabricCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
                    CommandSourceStack sender, Operator operator, MinecraftServer server) {
        super(formatter, interlocutor, AudienceRepresenter.identity(), sender, operator);
        this.server = server;
    }

    @Override
    public boolean hasPermission(String permission) {
        PermissionSet permissionSet = getRawSender().permissions();
        return permissionSet.hasPermission(Namespacing.parsePermission(permission))
                || permissionSet.hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS));
    }

    @Override
    public Stream<String> getPlayerNames() {
        return server.getPlayerList().getPlayers().stream().map(Player::getPlainTextName);
    }

    @Override
    public Stream<String> getPlayerNamesOnSameServer() {
        return getPlayerNames();
    }
}
