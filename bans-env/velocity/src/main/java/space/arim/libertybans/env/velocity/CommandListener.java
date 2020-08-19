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

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.api.proxy.Player;

class CommandListener extends VelocityParallelisedListener<CommandExecuteEvent, SendableMessage> {

	CommandListener(VelocityEnv env) {
		super(env);
	}
	
	@Override
	public void register() {
		register(CommandExecuteEvent.class);
	}

	@Override
	protected CentralisedFuture<SendableMessage> beginFor(CommandExecuteEvent evt) {
		CommandSource source = evt.getCommandSource();
		if (!(source instanceof Player)) {
			return null;
		}
		Player player = (Player) source;
		return env.core.getEnforcer().checkChat(player.getUniqueId(), player.getRemoteAddress().getAddress().getAddress(), evt.getCommand());
	}

	@Override
	protected void withdrawFor(CommandExecuteEvent evt) {
		SendableMessage message = withdraw(evt);
		if (message == null) {
			return;
		}
		evt.setResult(CommandResult.denied());
		env.handle.sendMessage(evt.getCommandSource(), message);
	}

}
