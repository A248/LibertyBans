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

package space.arim.libertybans.env.fabric.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.LibraryDetection;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public final class FabricMod implements ModInitializer,
        ServerLifecycleEvents.ServerStarting, CommandRegistrationCallback, ServerLifecycleEvents.ServerStopped {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CompletableFuture<BaseFoundation> initializationFuture;
    private BaseFoundation base;

	@Override
	public synchronized void onInitialize() {
        if (initializationFuture != null || base != null) {
            throw new IllegalStateException("Server initialised twice?");
        }
		initializationFuture = initialize();
        ServerLifecycleEvents.SERVER_STARTING.register(this);
        CommandRegistrationCallback.EVENT.register(this);
        ServerLifecycleEvents.SERVER_STOPPED.register(this);
	}

    @Override
    public synchronized void onServerStarting(MinecraftServer server) {
        logger.info("Server starting called");
        if (base == null) {
            return;
        }
        base.startup();
    }

    @Override
    public synchronized void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry,
                                      CommandManager.RegistrationEnvironment environment) {
        logger.info("Command registration called");
        if (initializationFuture != null) {
            try {
                base = initializationFuture.join();
            } finally {
                initializationFuture = null;
            }
        }
        if (base == null) {
            return;
        }
        PlatformAccess platformAccess = PlatformAccess.access(base);
        dispatcher.register(platformAccess.commandHandler());
    }

    @Override
    public void onServerStopped(MinecraftServer server) {
        BaseFoundation base = this.base;
        this.base = null;
        if (base == null) {
            logger.warn("LibertyBans wasn't launched; check your log for a startup error");
            return;
        }
        base.shutdown();
    }

    private CompletableFuture<BaseFoundation> initialize() {
        FabricLoader fabricLoader = FabricLoader.getInstance();
        ModMetadata fabricApi = fabricLoader.getModContainer("fabric-api").orElseThrow().getMetadata();
        ModContainer modContainer = fabricLoader.getModContainer(PluginInfo.ID).orElseThrow();

        LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
                .folder(fabricLoader.getConfigDir().resolve(PluginInfo.ID))
                .logger(new Slf4jBootstrapLogger(logger))
                .platform(Platform.builder(Platform.Category.FABRIC)
                        .nameAndVersion(fabricApi.getName(), fabricApi.getVersion().getFriendlyString())
                        .slf4jSupport(LibraryDetection.enabled())
                        .kyoriAdventureSupport(LibraryDetection.enabled())
                )
                .executor(ForkJoinPool.commonPool())
                .build();
        Payload<ModContainer> payload = launcher.getPayload(modContainer);
        return launcher.attemptLaunch().thenApply(launchLoader -> {
            BaseFoundation base;
            try {
                base = new Instantiator(
                        "space.arim.libertybans.env.fabric.FabricLauncher", launchLoader
                ).invoke(payload);
            } catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
                logger.warn("Failed to launch LibertyBans", ex);
                return null;
            }
            return base;
        });
    }
}