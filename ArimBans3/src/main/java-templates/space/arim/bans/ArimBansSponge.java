/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans;

import java.io.File;

import org.bstats.sponge.Metrics2;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.AsynchronousExecutor;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.SynchronousExecutor;

import com.google.inject.Inject;

import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.env.sponge.SpongeEnv;

import space.arim.universal.registry.Registry;
import space.arim.universal.registry.UniversalRegistry;
import space.arim.universal.util.lang.AutoClosable;

import space.arim.api.concurrent.AsyncExecution;
import space.arim.api.concurrent.Shutdownable;
import space.arim.api.concurrent.SyncExecution;
import space.arim.api.server.sponge.DefaultAsyncExecution;
import space.arim.api.server.sponge.DefaultSyncExecution;
import space.arim.api.server.sponge.DefaultUUIDResolver;
import space.arim.api.uuid.UUIDResolver;

@Plugin(id = "${plugin.spongeid}", name = "${plugin.name}", version = "${plugin.version}", authors = {"${plugin.author}"}, description = "${plugin.description}", url = "${plugin.url}", dependencies = {@Dependency(id = "arimapiplugin")})
public class ArimBansSponge implements AutoClosable {

	private ArimBans center;
	private SpongeEnv environment;
	
	@Inject
	@ConfigDir(sharedRoot=false)
	private File folder;
	
	private final Metrics2 metrics;
	
	private void load() {
		environment = new SpongeEnv(getPlugin());
		center = new ArimBansPlugin(getRegistry(), folder, environment);
		center.start();
		environment.loadFor(center);
		center.getRegistry().register(PunishmentPlugin.class, center);
		center.getRegistry().register(UUIDResolver.class, center.resolver());
	}
	
	@Override
	public void close() {
		center.close();
		environment.close();
	}
	
	private Registry getRegistry() {
		return center != null ? center.getRegistry() : UniversalRegistry.get();
	}
	
	private PluginContainer getPlugin() {
		return Sponge.getPluginManager().fromInstance(this).get();
	}
	
	@Inject
	public ArimBansSponge(Metrics2.Factory factory, @AsynchronousExecutor SpongeExecutorService async, @SynchronousExecutor SpongeExecutorService sync) {
		metrics = factory.make(6395);
		sync.execute(() -> {
			PluginContainer plugin = getPlugin();
			getRegistry().computeIfAbsent(AsyncExecution.class, () -> new DefaultAsyncExecution(plugin, async));
			getRegistry().computeIfAbsent(SyncExecution.class, () -> new DefaultSyncExecution(plugin, sync));
			getRegistry().computeIfAbsent(UUIDResolver.class, () -> new DefaultUUIDResolver(plugin));
		});
	}
	
	@Listener
	public void onEnable(GamePreInitializationEvent evt) {
		load();
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent evt) {
		metrics.addCustomChart(new Metrics2.SimplePie("storage_mode", () -> center.sql().getStorageModeName()));
		metrics.addCustomChart(new Metrics2.SimplePie("json_messages", () -> Boolean.toString(center.formats().useJson())));
	}
	
	@Listener
	public void onDisable(GameStoppingEvent evt) {
		AsyncExecution async = getRegistry().getRegistration(AsyncExecution.class);
		if (async instanceof Shutdownable) {
			((Shutdownable) async).shutdownAndWait();
		}
		close();
	}
	
}
