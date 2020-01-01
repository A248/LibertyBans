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
package space.arim.bans.api;

/**
 * Represents an answered request regarding a player's status.
 * 
 * @author A248
 *
 */
public class PunishmentResult {
	
	private final Subject subject;
	private final Punishment punishment;
	private final String message;
	
	public PunishmentResult(Subject subject, Punishment punishment, String message) {
		this.subject = subject;
		this.punishment = punishment;
		this.message = message;
	}
	
	/**
	 * Creates an empty object, indicating there is no applicable punishment.
	 * 
	 */
	public PunishmentResult() {
		subject = null;
		punishment = null;
		message = null;
	}
	
	/**
	 * Checks whether there is a punishment associated with this PunishmentResult
	 * 
	 * @return true if and only if the punishment is set
	 */
	public boolean hasPunishment() {
		return punishment != null;
	}
	
	/**
	 * Returns the subject about whom the PunishmentResult was asked
	 * 
	 * @return the subject
	 */
	public Subject getApplicableSubject() {
		return subject;
	}
	
	/**
	 * Returns a possible punishment, if applicable
	 * 
	 * @return the punishment, or null if not found
	 */
	public Punishment getApplicablePunishment() {
		return punishment;
	}
	
	/**
	 * Returns a possible message, if applicable
	 * 
	 * @return the message
	 */
	public String getApplicableMessage() {
		return message;
	}
	
}
