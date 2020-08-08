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

import java.util.UUID;

import space.arim.libertybans.core.env.AbstractOnlineTarget;
import space.arim.libertybans.core.env.Environment;

import com.velocitypowered.api.proxy.Player;

class VelocityOnlineTarget extends AbstractOnlineTarget {

	VelocityOnlineTarget(Environment env, Player player) {
		super(env, player);
	}

	@Override
	public boolean hasPermission(String permission) {
		return getRawPlayer().hasPermission(permission);
	}

	@Override
	public UUID getUniqueId() {
		return getRawPlayer().getUniqueId();
	}

	@Override
	public byte[] getAddress() {
		return getRawPlayer().getRemoteAddress().getAddress().getAddress();
	}
	
	@Override
	public Player getRawPlayer() {
		return (Player) super.getRawPlayer();
	}

}
