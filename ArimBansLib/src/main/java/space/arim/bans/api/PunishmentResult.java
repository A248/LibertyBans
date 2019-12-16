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

public class PunishmentResult {
	
	private final boolean has;
	private final Subject subject;
	private final Punishment punishment;
	private final String message;
	
	public PunishmentResult(Subject subject, Punishment punishment, String message) {
		has = true;
		this.subject = subject;
		this.punishment = punishment;
		this.message = message;
	}
	
	public PunishmentResult() {
		has = false;
		subject = null;
		punishment = null;
		message = null;
	}
	
	public boolean hasPunishment() {
		return has;
	}
	
	public Subject getApplicableSubject() {
		return subject;
	}
	
	public Punishment getApplicablePunishment() {
		return punishment;
	}
	
	public String getApplicableMessage() {
		return message;
	}
	
}
