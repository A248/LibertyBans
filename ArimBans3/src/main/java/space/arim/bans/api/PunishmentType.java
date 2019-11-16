package space.arim.bans.api;

import space.arim.bans.api.exception.TypeParseException;

public enum PunishmentType {
	BAN, MUTE, WARN, KICK;
	
	public String deserialise() {
		return this.toString();
	}
	
	public static PunishmentType serialise(String input) {
		for (PunishmentType type : PunishmentType.values()) {
			if (type.toString().equalsIgnoreCase(input)) {
				return type;
			}
		}
		throw new TypeParseException(input, PunishmentType.class);
	}
}
