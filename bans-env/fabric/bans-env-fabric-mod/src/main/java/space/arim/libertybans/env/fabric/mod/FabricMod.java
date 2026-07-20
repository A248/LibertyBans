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

package space.arim.libertybans.env.fabric.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.LibraryDetection;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.RunState;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public final class FabricMod implements ModInitializer,
        ServerLifecycleEvents.ServerStarting, CommandRegistrationCallback, ServerLifecycleEvents.ServerStopped {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private CompletableFuture<BaseFoundation> initializationFuture;
    private BaseFoundation base;

	@Override
	public synchronized void onInitialize() {
        LOGGER.debug("Initializing with debug logging...");
        if (initializationFuture != null || base != null) {
            throw new IllegalStateException("Server initialised twice?");
        }
		initializationFuture = initialize();
        ServerLifecycleEvents.SERVER_STARTING.register(this);
        CommandRegistrationCallback.EVENT.register(this);
        ServerLifecycleEvents.SERVER_STOPPED.register(this);
	}

    /**
     * Attempts to get the foundation: initializing and setting the server as appropriate
     *
     * @param purpose why it is needed
     * @param tryStart whether to enable startup via this call
     * @param server if non-null, will make sure that the implementation can see the server obect
     * @return the started foundation, or {@code null} if unable to start or not started
     */
    private BaseFoundation getBase(String purpose, boolean tryStart, @Nullable MinecraftServer server) {
        LOGGER.info("Called to {}", purpose);
        BaseFoundation base = this.base;
        if (initializationFuture != null) {
            try {
                this.base = base = initializationFuture.join();
            } finally {
                initializationFuture = null;
            }
        }
        if (base == null) {
            LOGGER.warn("LibertyBans never launched so it cannot {}.", purpose);
            return null;
        }
        if (server != null) {
            PlatformAccess.access(base).installServer(server);
        }
        boolean doStart = tryStart && server != null && base.getRunState() == RunState.IDLE;
        if (doStart) {
            base.startup();
        }
        if (base.getRunState() == RunState.FAILED) {
            LOGGER.warn("Unable to {} because LibertyBans failed to start", purpose);
            return null;
        }
        if (doStart) {
            PlatformAccess.Holder.install(base);
        }
        return base;
    }

    @Override
    public synchronized void onServerStarting(@NonNull MinecraftServer server) {
        getBase("start up", true, server);
    }

    @Override
    public synchronized void register(@NonNull CommandDispatcher<CommandSourceStack> dispatcher,
                                      @NonNull CommandBuildContext buildContext,
                                      Commands.@NonNull CommandSelection selection) {
        BaseFoundation base = getBase("register commands", false, null);
        if (base == null) {
            return;
        }
        PlatformAccess platformAccess = PlatformAccess.access(base);
        dispatcher.register(platformAccess.rootCommand());
    }

    @Override
    public synchronized void onServerStopped(@NonNull MinecraftServer server) {
        BaseFoundation base = getBase("shut down", false, server);
        this.base = null;
        if (base != null) {
            base.shutdown();
        }
    }

    private CompletableFuture<BaseFoundation> initialize() {
        FabricLoader fabricLoader = FabricLoader.getInstance();
        ModMetadata fabricApi = fabricLoader.getModContainer("fabric-api").orElseThrow().getMetadata();
        ModContainer modContainer = fabricLoader.getModContainer(PluginInfo.ID).orElseThrow();

        LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
                .folder(fabricLoader.getConfigDir().resolve(PluginInfo.ID))
                .logger(new Slf4jBootstrapLogger(LOGGER))
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
                LOGGER.warn("Failed to launch LibertyBans", ex);
                return null;
            }
            return base;
        });
    }
}