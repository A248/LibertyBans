/*
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;

import com.google.inject.Inject;

import space.arim.bans.extended.sponge.CommandSkeleton;
import space.arim.bans.extended.sponge.SignListener;

import space.arim.universal.registry.UniversalRegistry;

import space.arim.api.server.sponge.SpongeUtil;

@Plugin(id = "${plugin.spongeid}", name = "${plugin.name}", version = "${plugin.version}", authors = {"${plugin.author}"}, description = "${plugin.description}", url = "${plugin.url}")
public class ArimBansExtendedSponge implements ArimBansExtendedPlugin {

	@Inject
	@ConfigDir(sharedRoot=false)
	private File folder;
	
	private ArimBansExtended extended;
	private Set<CommandMapping> cmds = new HashSet<CommandMapping>();
	private SignListener listener;
	
	@Listener
	public void onEnable(@SuppressWarnings("unused") GamePreInitializationEvent evt) {
		extended = new ArimBansExtended(UniversalRegistry.get(), folder);
		loadCmds();
		loadAntiSign();
	}
	
	private void loadCmds() {
		for (String cmd : ArimBansExtended.commands()) {
			Sponge.getCommandManager().register(this, new CommandSkeleton(this, cmd), cmd).ifPresent(cmds::add);
		}
	}
	
	private void loadAntiSign() {
		if (extension().antiSignEnabled()) {
			listener = new SignListener(this);
			Sponge.getEventManager().registerListeners(this, listener);
		}
	}
	
	@Override
	public List<String> getTabComplete(String[] args) {
		return SpongeUtil.getPlayerNameTabComplete(args, Sponge.getServer());
	}
	
	@Listener
	public void onDisable(@SuppressWarnings("unused") GameStoppingServerEvent evt) {
		close();
	}
	
	@Override
	public ArimBansExtended extension() {
		return extended;
	}
	
	@Override
	public void close() {
		if (listener != null) {
			Sponge.getEventManager().unregisterListeners(listener);
		}
		cmds.forEach(Sponge.getCommandManager()::removeMapping);
		ArimBansExtendedPlugin.super.close();
	}

}
