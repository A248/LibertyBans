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
	
	final String prefix;
	
	private final String storageModeName;
	
	SqlSettings(ConfigMaster config, String storageModeName) {
		this.prefix = config.getConfigString("storage.table-prefix");
		this.storageModeName = storageModeName;
	}
	
	abstract HikariDataSource loadDataSource();
	
	//abstract Class<?> getDriverClass();
	
	HikariConfig getInitialConfig() {
		HikariConfig config = new HikariConfig();
		config.setMinimumIdle(2);
		config.setMaximumPoolSize(3);
		config.addDataSourceProperty("characterEncoding","utf8");
		config.addDataSourceProperty("useUnicode","true");
		//config.setDriverClassName(getDriverClass().getName());
		return config;
	}

	public String getStorageModeName() {
		return storageModeName;
	}

}
