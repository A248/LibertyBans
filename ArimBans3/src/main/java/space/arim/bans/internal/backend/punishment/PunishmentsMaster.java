package space.arim.bans.internal.backend.punishment;

import java.sql.ResultSet;
import java.util.Set;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.internal.Replaceable;

public interface PunishmentsMaster extends Replaceable {
	
	public void addPunishments(Punishment...punishments) throws ConflictingPunishmentException;

	public Punishment getPunishment(Subject subject, PunishmentType type) throws MissingPunishmentException;
	
	public void removePunishments(Punishment...punishments) throws MissingPunishmentException; 
	
	public boolean isBanned(Subject subject);

	public boolean isMuted(Subject subject);

	Set<Punishment> getWarns(Subject subject);
	
	Set<Punishment> getKicks(Subject subject);
	
	void loadActive(ResultSet data);
	
	void loadHistory(ResultSet data);
	
	void refreshActive();
}
