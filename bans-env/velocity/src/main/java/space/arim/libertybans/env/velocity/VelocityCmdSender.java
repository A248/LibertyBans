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

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.env.AbstractCmdSender;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

abstract class VelocityCmdSender extends AbstractCmdSender {
	
	VelocityCmdSender(VelocityEnv env, CommandSource sender, Operator operator) {
		super(env.core, sender, operator);
	}
	
	@Override
	public boolean hasPermission(String permission) {
		return getRawSender().hasPermission(permission);
	}

	@Override
	public CommandSource getRawSender() {
		return (CommandSource) super.getRawSender();
	}

}

class PlayerCmdSender extends VelocityCmdSender {

	PlayerCmdSender(VelocityEnv env, Player player) {
		super(env, player, PlayerOperator.of(player.getUniqueId()));
	}
	
}

class ConsoleCmdSender extends VelocityCmdSender {

	ConsoleCmdSender(VelocityEnv env, CommandSource sender) {
		super(env, sender, ConsoleOperator.INST);
	}
	
}
