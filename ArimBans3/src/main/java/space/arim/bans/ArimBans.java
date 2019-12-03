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

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.api.UUIDResolver;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.Configurable;
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
	
	default void loadData() {
		sql().executeQuery(new SqlQuery(SqlQuery.Query.CREATE_TABLE_CACHE.eval(sql().settings())), new SqlQuery(SqlQuery.Query.CREATE_TABLE_ACTIVE.eval(sql().settings())), new SqlQuery(SqlQuery.Query.CREATE_TABLE_HISTORY.eval(sql().settings())));
		ResultSet[] data = sql().selectQuery(new SqlQuery(SqlQuery.Query.SELECT_ALL_CACHED.eval(sql().settings())), new SqlQuery(SqlQuery.Query.SELECT_ALL_ACTIVE.eval(sql().settings())), new SqlQuery(SqlQuery.Query.SELECT_ALL_HISTORY.eval(sql().settings())));
		resolver().loadAll(data[0]);
		punishments().loadActive(data[1]);
		punishments().loadHistory(data[2]);
	}
	
	default void register() {
		UniversalRegistry.register(PunishmentPlugin.class, this);
		UniversalRegistry.register(UUIDResolver.class, resolver());
	}
	
	void log(String message);
	
	void logError(Exception ex);
	
	@Override
	default void refreshConfig(boolean first) {
		config().refreshConfig(first);
		sql().refreshConfig(first);
		punishments().refreshConfig(first);
		subjects().refreshConfig(first);
		resolver().refreshConfig(first);
		commands().refreshConfig(first);
		formats().refreshConfig(first);
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
	
}
