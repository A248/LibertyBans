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
package space.arim.bans.internal.backend.subjects;

import java.util.UUID;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.exception.TypeParseException;
import space.arim.bans.api.util.Tools;

public class Subjects implements SubjectsMaster {
	
	private final ArimBans center;
	
	private String console_display;

	public Subjects(ArimBans center) {
		this.center = center;
		refreshConfig();
	}
	
	@Override
	public String display(Subject subject) {
		if (subject.getType().equals(SubjectType.PLAYER)) {
			try {
				return center.environment().resolver().nameFromUUID(subject.getUUID());
			} catch (PlayerNotFoundException ex) {
				throw new InvalidSubjectException("No corresponding name found for player subject!", ex);
			}
		} else if (subject.getType().equals(SubjectType.IP)) {
			return subject.getIP();
		} else if (subject.getType().equals(SubjectType.CONSOLE)) {
			return console_display;
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	@Override
	public Subject parseSubject(String input) {
		if (center.checkAddress(input)) {
			return Subject.fromIP(input);
		} else if (input.length() == 36) {
			try {
				return parseSubject(UUID.fromString(input));
			} catch (IllegalArgumentException ex) {
				throw new InvalidUUIDException("UUID " + input + " does not conform.");
			}
		} else if (input.length() == 32) {
			return parseSubject(Tools.expandUUID(input));
		} else if (input.equalsIgnoreCase(console_display)) {
			return console();
		}
		throw new TypeParseException(input, Subject.class);
	}
	
	@Override
	public Subject parseSubject(UUID input) {
		if (checkUUID(input)) {
			return Subject.fromUUID(input);
		}
		throw new InvalidUUIDException(input);
	}
	
	@Override
	public boolean checkUUID(UUID uuid) {
		return center.cache().uuidExists(uuid);
	}
	
	/**
	 * Identical to {@link Subject#console()}
	 */
	public Subject console() {
		return Subject.console();
	}
	
	@Override
	public void close() {
		
	}

	@Override
	public void refreshConfig() {
		console_display = center.config().getString("formatting.console-display");
	}
}
