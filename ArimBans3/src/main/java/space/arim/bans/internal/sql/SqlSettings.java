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
package space.arim.bans.internal.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import space.arim.bans.internal.config.ConfigMaster;

public abstract class SqlSettings {
	
	final int min_connections;
	final int max_connections;
	
	public final StorageMode mode;
	final String prefix;
	
	protected SqlSettings(ConfigMaster config, StorageMode mode) {
		this.min_connections = config.getConfigInt("storage.min-connections");
		this.max_connections = config.getConfigInt("storage.max-connections");
		this.mode = mode;
		this.prefix = config.getConfigString("storage.table-prefix");
	}
	
	abstract HikariDataSource loadDataSource();
	
	protected HikariConfig getInitialConfig() {
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("com.mysql.jdbc.Driver");
		config.setMinimumIdle(min_connections);
		config.setMaximumPoolSize(max_connections);
		return config;
	}

}
