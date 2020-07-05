/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.spigot;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.env.CmdSender;

abstract class SpigotCmdSender implements CmdSender {

	final SpigotEnv env;
	final CommandSender sender;
	private final Operator operator;
	
	SpigotCmdSender(SpigotEnv env, CommandSender sender, Operator operator) {
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

class PlayerCmdSender extends SpigotCmdSender {

	PlayerCmdSender(SpigotEnv env, Player player) {
		super(env, player, PlayerOperator.of(player.getUniqueId()));
	}
	
	
}

class ConsoleCmdSender extends SpigotCmdSender {

	ConsoleCmdSender(SpigotEnv env, CommandSender sender) {
		super(env, sender, ConsoleOperator.INST);
	}
	
}
