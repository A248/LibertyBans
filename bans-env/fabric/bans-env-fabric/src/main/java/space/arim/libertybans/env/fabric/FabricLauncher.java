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

import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.LoggerFactory;
import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.PlatformId;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.omnibus.Omnibus;

import java.nio.file.Path;

public final class FabricLauncher implements PlatformLauncher {

    private final Payload<ModContainer> payload;
    private final MinecraftServer server;
    private final Omnibus omnibus;

    public FabricLauncher(Payload<ModContainer> payload, MinecraftServer server, Omnibus omnibus) {
        this.payload = payload;
        this.server = server;
        this.omnibus = omnibus;
    }

    @Override
    public BaseFoundation launch() {
        Thread mainThread = Thread.currentThread();
        LoggerFactory.getLogger(getClass()).info(
                "Using thread '{}' as the main thread. Details: {}", mainThread.getName(), mainThread
        );
        return new InjectorBuilder()
                .bindInstance(ModContainer.class, payload.plugin())
                .bindInstance(MinecraftServer.class, server)
                .bindInstance(PlatformId.class, payload.platformId())
                .bindInstance(Identifier.ofTypeAndNamed(Thread.class, "mainThread"), mainThread)
                .bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), payload.pluginFolder())
                .bindInstance(InstanceType.class, InstanceType.GAME_SERVER)
                .bindInstance(Omnibus.class, omnibus)
                .addBindModules(
                        new ApiBindModule(),
                        new PillarOneBindModule(),
                        new PillarTwoBindModule(),
                        new CommandsModule(),
                        new FabricBindModule()
                )
                .specification(SpecificationSupport.JAKARTA)
                .multiBindings(true)
                .build()
                .request(BaseFoundation.class);
    }
}
