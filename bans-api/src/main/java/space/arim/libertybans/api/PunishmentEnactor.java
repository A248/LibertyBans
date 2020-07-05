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

import space.arim.universal.util.concurrent.CentralisedFuture;

public interface PunishmentEnactor {
	
	/**
	 * Enacts a punishment, adding it to the database. <br>
	 * If the punishment type is a ban or mute, and there is already a ban or mute for the user,
	 * the future will yield {@code null}.
	 * 
	 * @param draftPunishment the draft punishment to enact
	 * @return a centralised future which yields the punishment or {@code null} if there was a conflict
	 */
	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment);
	
	/**
	 * Undoes an existing punishment in the database. <br>
	 * If the punishment existed and was removed, the future yields {@code true}, else {@code false}.
	 * 
	 * @param punishment the punishment to undo
	 * @return a centralised future which yields {@code true} if the punishment existing and was removed, {@code false} otherwise
	 */
	CentralisedFuture<Boolean> undoPunishment(Punishment punishment);
	
	/**
	 * Enforces a punishment. For example, a kick will kick the player. A mute will prevent the player from further chatting. <br>
	 * This should be called after {@link #enactPunishment(DraftPunishment)} assuming the caller wants the punishment to be
	 * enforced.
	 * 
	 * @param punishment the punishment to begin enforcing
	 */
	void enforcePunishment(Punishment punishment);
	
}
