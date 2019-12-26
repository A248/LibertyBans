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

import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.env.Environment;
import space.arim.bans.internal.async.Async;
import space.arim.bans.internal.async.AsyncMaster;
import space.arim.bans.internal.async.AsyncWrapper;
import space.arim.bans.internal.backend.punishment.Corresponder;
import space.arim.bans.internal.backend.punishment.Punishments;
import space.arim.bans.internal.backend.resolver.Resolver;
import space.arim.bans.internal.backend.subjects.Subjects;
import space.arim.bans.internal.config.Config;
import space.arim.bans.internal.frontend.commands.Commands;
import space.arim.bans.internal.frontend.format.Formats;
import space.arim.bans.internal.logging.Logs;
import space.arim.bans.internal.sql.Sql;
import space.arim.bans.internal.sql.SqlQuery;

import space.arim.universal.registry.UniversalRegistry;

import space.arim.api.AsyncExecutor;

public class ArimBansPlugin implements ArimBans {
	
	private final File folder;
	private final Environment environment;
	private final Logs logs;
	private final Config config;
	private final Sql sql;
	private final Punishments punishments;
	private final Subjects subjects;
	private final Resolver resolver;
	private final Commands commands;
	private final Formats formats;
	private final Corresponder corresponder;
	private final AsyncMaster async;
	
	private boolean started = false;
	
	ArimBansPlugin(File folder, Environment environment) {
		this.folder = folder;
		this.environment = environment;
		config = new Config(this);
		logs = new Logs(this);
		sql = new Sql(this);
		punishments = new Punishments(this);
		subjects = new Subjects(this);
		resolver = new Resolver(this);
		commands = new Commands(this);
		formats = new Formats(this);
		corresponder = new Corresponder(this);
		AsyncExecutor registeredAsync = UniversalRegistry.get().getRegistration(AsyncExecutor.class);
		if (registeredAsync != null) {
			async = new AsyncWrapper(registeredAsync);
		} else {
			async = new Async(this);
			UniversalRegistry.get().register(AsyncExecutor.class, (AsyncExecutor) async);
		}
	}
	
	@Override
	public void start() {
		if (!started) {
			started = true;
			refresh(true);
			loadData();
		} else {
			throw new InternalStateException("#start cannot be called because ArimBans is already started!");
		}
	}
	
	private void loadData() {
		sql().executeQuery(new SqlQuery(SqlQuery.Query.CREATE_TABLE_CACHE), new SqlQuery(SqlQuery.Query.CREATE_TABLE_ACTIVE), new SqlQuery(SqlQuery.Query.CREATE_TABLE_HISTORY));
		ResultSet[] data = sql().selectQuery(new SqlQuery(SqlQuery.Query.SELECT_ALL_CACHED), new SqlQuery(SqlQuery.Query.SELECT_ALL_ACTIVE), new SqlQuery(SqlQuery.Query.SELECT_ALL_HISTORY));
		resolver().loadAll(data[0]);
		punishments().loadActive(data[1]);
		punishments().loadHistory(data[2]);
	}

	@Override
	public File dataFolder() {
		return folder;
	}
	
	@Override
	public Environment environment() {
		return environment;
	}
	
	@Override
	public Config config() {
		return config;
	}
	
	@Override
	public Logs logs() {
		return logs;
	}
	
	@Override
	public Sql sql() {
		return sql;
	}
	
	@Override
	public Punishments punishments() {
		return punishments;
	}

	@Override
	public Subjects subjects() {
		return subjects;
	}

	@Override
	public Resolver resolver() {
		return resolver;
	}

	@Override
	public Commands commands() {
		return commands;
	}

	@Override
	public Formats formats() {
		return formats;
	}
	
	@Override
	public Corresponder corresponder() {
		return corresponder;
	}
	
	@Override
	public void async(Runnable command) {
		async.execute(command);
	}

}