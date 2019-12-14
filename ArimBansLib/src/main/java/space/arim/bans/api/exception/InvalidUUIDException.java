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

import java.util.UUID;

public class InvalidUUIDException extends InternalStateException {
	
	private static final long serialVersionUID = 3047495662110600474L;
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified UUID. Used when UUID does not match to any player.
     *
     * @param   uuid   the uuid.
     */
	public InvalidUUIDException(UUID uuid) {
		this("UUID " + uuid.toString() + " does not match any player!");
	}
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified UUID and cause. Used when UUID does not match to any player.
     *
     * @param   uuid   the UUID
     * @param   cause  the cause
     */
	public InvalidUUIDException(UUID uuid, Exception cause) {
		this("UUID " + uuid.toString() + " does not match any player!", cause);
	}
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified detail message.
     *
     * @param   message   the detail message.
     */
	public InvalidUUIDException(String message) {
		super(message);
	}
	
	/**
     * Constructs an <code>InvalidUUIDException</code> with the
     * specified detail message and cause.
     *
     * @param   message  the detail message.
     * @param   cause    the cause
     */
	public InvalidUUIDException(String message, Exception cause) {
		super(message, cause);
	}

}
