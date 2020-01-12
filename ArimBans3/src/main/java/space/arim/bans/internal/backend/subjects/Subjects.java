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
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InvalidUUIDException;

import space.arim.api.uuid.UUIDUtil;

public class Subjects implements SubjectsMaster {
	
	private final ArimBans center;
	
	private static final int LENGTH_OF_FULL_UUID = 36;
	private static final int LENGTH_OF_SHORT_UUID = 32;

	private boolean op_permissions = true;
	private boolean usePrefix = true;
	private String prefix = "Prefix>> ";

	public Subjects(ArimBans center) {
		this.center = center;
	}
	
	@Override
	public boolean isOnline(Subject subject) {
		return center.environment().isOnline(subject);
	}
	
	@Override
	public Subject parseSubject(String input, boolean console) throws IllegalArgumentException {
		if (center.checkAddress(input)) {
			return Subject.fromIP(input);
		} else if (input.length() == LENGTH_OF_FULL_UUID) {
			try {
				return parseSubject(UUID.fromString(input));
			} catch (IllegalArgumentException ex) {
				throw new InvalidUUIDException("UUID " + input + " does not conform.");
			}
		} else if (input.length() == LENGTH_OF_SHORT_UUID) {
			return parseSubject(UUIDUtil.expand(input));
		} else if (console && input.equalsIgnoreCase(center.formats().getConsoleDisplay())) {
			return Subject.console();
		}
		throw new IllegalArgumentException("Could not make " + input + " into a subject");
	}
	
	@Override
	public Subject parseSubject(UUID input) {
		if (checkUUID(input)) {
			return Subject.fromUUID(input);
		}
		throw new InvalidUUIDException(input);
	}
	
	@Override
	public void sendMessage(Subject subject, boolean prefixed, String...jsonables) {
		boolean json = center.formats().useJson();
		for (int n = 0; n < jsonables.length; n++) {
			ArimBansLibrary.checkString(jsonables[n]);
			center.environment().sendMessage(subject, addQuotes((n == 0 && usePrefix && !prefixed) ? prefix + jsonables[n] : jsonables[n]), json);
		}
	}
	
	@Override
	public boolean hasPermission(Subject subject, String permission) {
		return ArimBansLibrary.checkString(permission) && center.environment().hasPermission(subject, permission, op_permissions);
	}
	
	private String notifyPerm(PunishmentType type) {
		return "arimbans." + type.name() + ".notify";
	}
	
	@Override
	public void sendNotif(Punishment punishment, boolean add, Subject operator) {
		String msg = center.formats().formatNotification(punishment, add, operator);
		ArimBansLibrary.checkString(msg);
		center.environment().sendMessage(notifyPerm(punishment.type()), addQuotes((usePrefix) ? prefix + msg : msg), center.formats().useJson());
	}
	
	private String addQuotes(String message) {
		return message.replace("%APOS%", "'").replace("%QUOTE%", "\"");
	}
	
	@Override
	public boolean checkUUID(UUID uuid) {
		return center.resolver().uuidExists(uuid);
	}
	
	@Override
	public void refreshConfig(boolean first) {
		op_permissions = center.config().getConfigBoolean("commands.op-permissions");
		usePrefix = center.config().getMessagesBoolean("all.prefix.use");
		prefix = center.config().getMessagesString("all.prefix.value");
	}
	
}
