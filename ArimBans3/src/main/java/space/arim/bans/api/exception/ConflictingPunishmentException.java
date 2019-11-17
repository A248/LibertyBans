/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.exception;

import space.arim.bans.api.Subject;
import space.arim.bans.api.PunishmentType;

public class ConflictingPunishmentException extends InternalStateException {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = -1375684402484585054L;

	public ConflictingPunishmentException(Subject subject, PunishmentType type) {
		super("Subject " + subject + " already has a punishment " + type.deserialise());
	}
	
}
