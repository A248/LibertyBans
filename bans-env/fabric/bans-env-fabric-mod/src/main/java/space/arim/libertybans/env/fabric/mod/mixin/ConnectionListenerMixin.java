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

package space.arim.libertybans.env.fabric.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import space.arim.libertybans.env.fabric.mod.PlatformAccess;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ConnectionListenerMixin {

    @Shadow
    @Final
    private Connection connection;

    @Shadow
    public abstract void disconnect(Component component);

    @WrapMethod(method = "startClientVerification", require = 1)
    private void checkIncomingConnection(GameProfile authenticatedProfile, Operation<Void> original) {
        PlatformAccess platformAccess =  PlatformAccess.Holder.getInstance();
        if (platformAccess == null) {
            original.call(authenticatedProfile);
            return;
        }
        Component denialMessage = platformAccess.checkConnection(
                authenticatedProfile.id(), connection.getRemoteAddress(), authenticatedProfile.name()
        );
        if (denialMessage == null) {
            original.call(authenticatedProfile);
        } else {
            disconnect(denialMessage);
            // Canceled
        }
    }
}
