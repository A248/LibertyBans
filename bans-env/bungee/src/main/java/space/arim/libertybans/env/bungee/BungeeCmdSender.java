/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.env.CmdSender;

abstract class BungeeCmdSender implements CmdSender {

	final BungeeEnv env;
	final CommandSender sender;
	private final Operator operator;
	
	BungeeCmdSender(BungeeEnv env, CommandSender sender, Operator operator) {
		this.env = env;
		this.sender = sender;
		this.operator = operator;
	}
	
	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public boolean hasPermission(String permission) {
		return sender.hasPermission(permission);
	}

	@Override
	public CommandSender getRawSender() {
		return sender;
	}
	
	@Override
	public void sendMessage(String jsonable) {
		env.sendJson(sender, jsonable);
	}
	
}

class PlayerCmdSender extends BungeeCmdSender {

	PlayerCmdSender(BungeeEnv env, ProxiedPlayer player) {
		super(env, player, PlayerOperator.of(player.getUniqueId()));
	}
	
	
}

class ConsoleCmdSender extends BungeeCmdSender {

	ConsoleCmdSender(BungeeEnv env, CommandSender sender) {
		super(env, sender, ConsoleOperator.INST);
	}
	
}
