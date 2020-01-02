/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.exception;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;

public class MissingPunishmentException extends Exception {

	private static final long serialVersionUID = -3749852643537426416L;

	private final Punishment punishment;
	
	public MissingPunishmentException(Punishment fakePunishment) {
		super("Punishment with these details does not exist: " + fakePunishment.type() + "/" + fakePunishment.subject() + "/" + fakePunishment.operator() + "/" + fakePunishment.expiration() + "/" + fakePunishment.date());
		punishment = fakePunishment;
	}
	
	public MissingPunishmentException(Subject subject, PunishmentType type) {
		super("Subject " + subject + " does not have a punishment of type " + type);
		punishment = null;
	}
	
	public MissingPunishmentException(int id) {
		super("Punishment with id " + id + " does not exist");
		punishment = null;
	}
	
	public MissingPunishmentException() {
		punishment = null;
	}

	public MissingPunishmentException(Exception ex) {
		super(ex);
		punishment = null;
	}

	public Punishment getNonExistentPunishment() {
		return punishment;
	}
	
}
