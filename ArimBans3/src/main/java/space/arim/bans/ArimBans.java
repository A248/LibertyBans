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
package space.arim.bans;

import java.io.File;
import java.sql.ResultSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.UUIDResolver;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.Configurable;
import space.arim.bans.internal.backend.punishment.CorresponderMaster;
import space.arim.bans.internal.backend.punishment.PunishmentsMaster;
import space.arim.bans.internal.backend.resolver.ResolverMaster;
import space.arim.bans.internal.backend.subjects.SubjectsMaster;
import space.arim.bans.internal.config.ConfigMaster;
import space.arim.bans.internal.frontend.commands.CommandsMaster;
import space.arim.bans.internal.frontend.format.FormatsMaster;
import space.arim.bans.internal.sql.SqlMaster;
import space.arim.bans.internal.sql.SqlQuery;

import space.arim.registry.UniversalRegistry;
import space.arim.registry.RegistryPriority;

public interface ArimBans extends Configurable, ArimBansLibrary {

	File dataFolder();
	
	Environment environment();
	
	ConfigMaster config();
	
	SqlMaster sql();
	
	PunishmentsMaster punishments();
	
	SubjectsMaster subjects();
	
	ResolverMaster resolver();
	
	CommandsMaster commands();
	
	FormatsMaster formats();
	
	CorresponderMaster corresponder();
	
	default void loadData() {
		sql().executeQuery(new SqlQuery(SqlQuery.Query.CREATE_TABLE_CACHE), new SqlQuery(SqlQuery.Query.CREATE_TABLE_ACTIVE), new SqlQuery(SqlQuery.Query.CREATE_TABLE_HISTORY));
		ResultSet[] data = sql().selectQuery(new SqlQuery(SqlQuery.Query.SELECT_ALL_CACHED), new SqlQuery(SqlQuery.Query.SELECT_ALL_ACTIVE), new SqlQuery(SqlQuery.Query.SELECT_ALL_HISTORY));
		resolver().loadAll(data[0]);
		punishments().loadActive(data[1]);
		punishments().loadHistory(data[2]);
	}
	
	default void register() {
		UniversalRegistry.register(PunishmentPlugin.class, this);
		UniversalRegistry.register(UUIDResolver.class, resolver());
	}
	
	void log(Level level, String message);
	
	void logError(Exception ex);
	
	default void checkDeleteLogs() {
		long keepAlive = 86_400_000L * config().getConfigInt("storage.log-keep-alive");
		long current = System.currentTimeMillis();
		for (File dir : (new File(dataFolder().getPath(), "logs")).listFiles()) {
			if (dir.isDirectory() && (current - dir.lastModified() > keepAlive)) {
				if (!dir.delete()) {
					log(Level.WARNING, "Could not delete old logs folder!");
				} else {
					log(Level.FINER, "Deleted old log folder " + dir.getName());
				}
			}
		}
	}
	
	@Override
	default void refreshConfig(boolean first) {
		config().refreshConfig(first);
		sql().refreshConfig(first);
		punishments().refreshConfig(first);
		subjects().refreshConfig(first);
		resolver().refreshConfig(first);
		commands().refreshConfig(first);
		formats().refreshConfig(first);
		corresponder().refreshConfig(first);
	}
	
	@Override
	default void refreshMessages(boolean first) {
		config().refreshMessages(first);
		sql().refreshMessages(first);
		punishments().refreshMessages(first);
		subjects().refreshMessages(first);
		resolver().refreshMessages(first);
		commands().refreshMessages(first);
		formats().refreshMessages(first);
		corresponder().refreshMessages(first);
	}
	
	@Override
	default void close() {
		config().close();
		sql().close();
		punishments().close();
		subjects().close();
		commands().close();
		resolver().close();
		formats().close();
		corresponder().close();
	}
	
	@Override
	default String getName() {
		return environment().getName();
	}
	
	@Override
	default String getAuthor() {
		return environment().getAuthor();
	}
	
	@Override
	default String getVersion() {
		return environment().getVersion();
	}
	
	@Override
	default byte getPriority() {
		return RegistryPriority.LOWER;
	}
	
	@Override
	default int getNextAvailablePunishmentId() {
		return punishments().getNextAvailablePunishmentId();
	}
	
	@Override
	default PunishmentResult getApplicableBan(UUID uuid, String address) {
		return corresponder().getApplicablePunishment(uuid, address, PunishmentType.BAN);
	}
	
	@Override
	default PunishmentResult getApplicableMute(UUID uuid, String address) {
		return corresponder().getApplicablePunishment(uuid, address, PunishmentType.MUTE);
	}
	
	@Override
	default Set<Punishment> getApplicableWarns(UUID uuid, String address) {
		return corresponder().getApplicablePunishments(uuid, address, PunishmentType.WARN);
	}
	
	@Override
	default Set<Punishment> getApplicableKicks(UUID uuid, String address) {
		return corresponder().getApplicablePunishments(uuid, address, PunishmentType.KICK);
	}
	
	@Override
	default Set<Punishment> getPunishmentsActive() {
		return punishments().getActive();
	}
	
	@Override
	default Set<Punishment> getPunishmentsHistory() {
		return punishments().getHistory();
	}
	
	@Override
	default void addPunishments(Punishment...punishments) throws ConflictingPunishmentException {
		punishments().addPunishments(punishments);
	}
	
	@Override
	default void removePunishments(Punishment...punishments) {
		try {
			punishments().removePunishments(punishments);
		} catch (MissingPunishmentException ex) {
			logError(ex);
		}
	}
	
	@Override
	default Subject fromUUID(UUID subject) {
		return Subject.fromUUID(subject);
	}
	
	@Override
	default Subject fromIpAddress(String address) throws IllegalArgumentException {
		return Subject.fromIP(address);
	}
	
	@Override
	default Subject parseSubject(String input) throws IllegalArgumentException {
		return subjects().parseSubject(input);
	}
	
	@Override
	default void simulateCommand(Subject subject, CommandType command, String[] args) {
		commands().execute(subject, command, args);
	}
	
	@Override
	default void simulateCommand(Subject subject, String[] rawArgs) {
		commands().execute(subject, rawArgs);
	}
	
	@Override
	default boolean asynchronous() {
		return corresponder().asynchronous();
	}
	
	@Override
	default void reload() {
		refresh(false);
	}
	
	@Override
	default void reloadConfig() {
		refreshConfig(false);
	}
	
	@Override
	default void reloadMessages() {
		refreshMessages(false);
	}
	
	@Override
	default void sendMessage(Subject subject, String message) {
		subjects().sendMessage(subject, message);
	}
	
}
