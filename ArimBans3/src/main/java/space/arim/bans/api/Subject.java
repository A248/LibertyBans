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
package space.arim.bans.api;

import java.util.UUID;

import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.exception.TypeParseException;
import space.arim.bans.api.util.ToolsUtil;

/**
 * 
 * Subjects can represent 3 things:
 * 1. Players
 * 2. IP addresses
 * 3. The console
 * 
 * @author anandbeh
 *
 */
public class Subject {

	private final UUID uuid;
	private final String ip;
	private final SubjectType type;
	
	private static final Subject CONSOLE = new Subject(SubjectType.CONSOLE);
	
	protected Subject(UUID uuid) {
		this.type = SubjectType.PLAYER;
		this.uuid = uuid;
		this.ip = null;
	}
	
	protected Subject(String ip) {
		this.type = SubjectType.IP;
		this.uuid = null;
		this.ip = ip;
	}
	
	protected Subject(SubjectType type) {
		this.type = type;
		this.uuid = null;
		this.ip = null;
	}
	
	/**
	 * Creates a Subject from a player UUID
	 * 
	 * <br><br><b>You need to check the UUID first.</b>
	 * See {@link ArimBansLibrary#checkUUID(UUID)}
	 * 
	 * @param uuid - the uuid to use
	 * @return Subject representing the uuid
	 */
	public static Subject fromUUID(UUID playeruuid) throws IllegalArgumentException {
		return new Subject(playeruuid);
	}
	
	/**
	 * Creates a Subject from an IP address
	 * 
	 * <br><br><b>You need to validate the address first.</b>
	 * See {@link ArimBansLibrary#checkAddress(String)}
	 * 
	 * @param address - the IP address to create a Subject from
	 * @return Subject representing the ip specified
	 * @throws IllegalArgumentException if the parameter is not a valid ipv4 or ipv6 address
	 */
	public static Subject fromIP(String address) {
		if (!ToolsUtil.validAddress(address)) {
			throw new IllegalArgumentException("Could not make " + address + " into a subject because it is not a valid IP address!");
		}
		return new Subject(address);
	}
	
	/**
	 * Gets the console
	 * 
	 * @return
	 */
	public static Subject console() {
		return CONSOLE;
	}

	/**
	 * Gets a Subject's UUID
	 * Returns null if {@link #getType()} != SubjectType.Player
	 * 
	 * @return the corresponding player's UUID
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Gets a Subject's IP Address
	 * Returns null if {@link #getType()} != SubjectType.IP
	 * 
	 * @return the ip address represented by this Subject
	 */
	public String getIP() {
		return this.ip;
	}
	
	/**
	 * Checks whether this subject is the server console
	 * 
	 * @return boolean if the specified subject is the console
	 */
	public boolean isConsole() {
		return this.type.equals(SubjectType.CONSOLE);
	}

	/**
	 * The type of this Subject
	 * 
	 * @return SubjectType the type
	 * 
	 * @see SubjectType
	 */
	public SubjectType getType() {
		return this.type;
	}

	/**
	 * Determines what kind of Subject a Subject is
	 * 
	 * @author anandbeh
	 *
	 */
	public enum SubjectType {
		PLAYER, IP, CONSOLE
	}
	
	/**
	 * Serialises a subject
	 * 
	 * @param input a string representation of a subject as determined by {@link #deserialise()}
	 * @return a serialised Subject
	 * 
	 * @throws InvalidUUIDException if input string reports a uuid but does not conform to UUID representation
	 * @throws TypeParseException with parameter Subject.class if input string is in valid
	 */
	public static Subject serialise(String input) throws InvalidUUIDException, TypeParseException {
		if (input.startsWith("[subject:uuid]")) {
			try {
				return new Subject(UUID.fromString(ToolsUtil.expandUUID(input.substring(14))));
			} catch (IllegalArgumentException ex) {
				throw new InvalidUUIDException("UUID " + input + " does not conform.");
			}
		} else if (input.startsWith("[subject:addr]")) {
			return new Subject(input.substring(14));
		} else if (input.equalsIgnoreCase("[subject:cons]")) {
			return new Subject(SubjectType.CONSOLE);
		}
		throw new TypeParseException(input, Subject.class);
	}
	
	/**
	 * Deserialises this subject
	 * 
	 * @return string representation of subject
	 * @throws InvalidSubjectException for a broken subject
	 */
	public String deserialise() throws InvalidSubjectException {
		switch (this.type) {
		case PLAYER:
			return "[subject:uuid]" + this.uuid.toString().replaceAll("-", "");
		case IP:
			return "[subject:addr]" + this.ip;
		case CONSOLE:
			return "[subject:cons]";
		default:
			throw new InvalidSubjectException(this);
		}		
	}

	/**
	 * Returns true if subjects are equal in their properties.
	 * 
	 * @throws InvalidSubjectException for broken subjects.
	 */
	public boolean compare(Subject otherSubject) throws InvalidSubjectException {
		if (getType().equals(otherSubject.getType())) {
			switch (getType()) {
			case PLAYER:
				return (getUUID().equals(otherSubject.getUUID()));
			case IP:
				return (getIP().equals(otherSubject.getIP()));
			case CONSOLE:
				return true;
			default:
				throw new InvalidSubjectException(this);
			}
		}
		return false;
	}
}
