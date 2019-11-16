package space.arim.bans.api;

import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.ConflictingPunishmentException;

public class ArimBansLibrary {
	
	private ArimBans center;
	
	public ArimBansLibrary(ArimBans center) {
		this.center = center;
	}
	
	public boolean isBanned(Subject subject) {
		return center.manager().isBanned(subject);
	}
	
	public boolean isMuted(Subject subject) {
		return center.manager().isMuted(subject);
	}
	
	public int countWarns(Subject subject) {
		return getWarns(subject).size();
	}
	
	public Set<Punishment> getWarns(Subject subject) {
		return center.manager().getWarns(subject);
	}
	
	public void addPunishments(Punishment...punishments) throws ConflictingPunishmentException {
		center.manager().addPunishments(punishments);
	}
	
	public Subject fromUUID(UUID subject) {
		return center.subjects().parseSubject(subject);
	}
	
	public Subject fromPlayer(Player subject) {
		return center.subjects().parseSubject(subject.getUniqueId());
	}
	
	/**
	 * Gets a Subject from an IP Address
	 * 
	 * @param address - the address to use
	 * @return a Subject representing the address
	 * @throws IllegalArgumentException if address is invalid according to ApacheCommons InetAddressValidator
	 */
	public Subject fromIpAddress(String address) throws IllegalArgumentException {
		Subject subject = center.subjects().parseSubject(address);
		if (subject.getType().equals(SubjectType.IP)) {
			return subject;
		}
		throw new IllegalArgumentException("Could not make " + address + " into a subject because it is not a valid IP address! To avoid this error, surround your API call in a try/catch statement.");
	}
	
	public Subject getConsole() {
		return Subject.console();
	}
	
	public void simulateCommand(Subject subject, CommandType command, String[] args) {
		center.commands().execute(subject, command, args);
	}
	
	public boolean checkUUID(UUID uuid) {
		return center.subjects().checkUUID(uuid);
	}
	
	public boolean checkAddress(String address) {
		return Tools.validAddress(address);
	}

}
