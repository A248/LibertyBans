/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

@Singleton
public class CommandListener extends VelocityParallelisedListener<CommandExecuteEvent, Component> {

	private final Enforcer enforcer;

	@Inject
	public CommandListener(PluginContainer plugin, ProxyServer server, Enforcer enforcer) {
		super(plugin, server);
		this.enforcer = enforcer;
	}

	@Override
	public Class<CommandExecuteEvent> getEventClass() {
		return CommandExecuteEvent.class;
	}

	@Override
	protected boolean skipEvent(CommandExecuteEvent event) {
		return !(event.getCommandSource() instanceof Player);
	}

	@Override
	protected CentralisedFuture<Component> beginComputation(CommandExecuteEvent event) {
		Player player = (Player) event.getCommandSource();
		return enforcer.checkChat(player.getUniqueId(), player.getRemoteAddress().getAddress(), event.getCommand());
	}

	@Override
	protected void executeNonNullResult(CommandExecuteEvent event, Component message) {
		event.setResult(CommandResult.denied());
		event.getCommandSource().sendMessage(message);
	}

}
