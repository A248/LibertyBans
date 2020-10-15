/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.env;

import java.util.UUID;
import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.LibertyBansCore;

public abstract class AbstractEnvEnforcer implements EnvEnforcer {

	private final LibertyBansCore core;
	private final Environment env;
	
	protected AbstractEnvEnforcer(LibertyBansCore core, Environment env) {
		this.core = core;
		this.env = env;
	}
	
	protected Environment env() {
		return env;
	}
	
	@Override
	public final void sendToThoseWithPermission(String permission, SendableMessage message) {
		sendToThoseWithPermission0(permission, core.getFormatter().prefix(message));
	}
	
	protected abstract void sendToThoseWithPermission0(String permission, SendableMessage message);

	@Override
	public void kickByUUID(UUID uuid, SendableMessage message) {
		doForPlayerIfOnline(uuid, (player) -> env.getPlatformHandle().disconnectUser(player, message));
	}

	@Override
	public void sendMessageByUUID(UUID uuid, SendableMessage message) {
		doForPlayerIfOnline(uuid, (player) -> env.getPlatformHandle().sendMessage(player, message));
	}
	
}
