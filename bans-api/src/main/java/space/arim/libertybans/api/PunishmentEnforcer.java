/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

/**
 * Enforcer of punishments
 * 
 * @author A248
 *
 */
public interface PunishmentEnforcer {

	/**
	 * Enforces a punishment. <br>
	 * <br>
	 * For bans and mutes, this will kick players matching the punishment's
	 * victim. For mutes and warn, the players will be sent a warning message. <br>
	 * Additionally for mutes, the player will be unable to chat (depending on the implementation)
	 * until the mute cache expires, at which point the database is re-queried for a mute.
	 * 
	 * @param punishment the punishment to enforce
	 * @return a future completed when enforcement has been conducted
	 */
	CentralisedFuture<?> enforce(Punishment punishment);
	
}
