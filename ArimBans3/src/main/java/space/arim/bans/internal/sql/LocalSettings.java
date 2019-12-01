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

public class LocalSettings extends SqlSettings {
	
	private final String url;
	
	public LocalSettings(ConfigMaster config) {
		super(config, StorageMode.HSQLDB);
		url = config.getConfigString("storage.file.url").replace("%FILE%", config.getDataFolder().getPath() + "/" + config.getConfigString("storage.file.filename"));
	}

	@Override
	HikariDataSource loadDataSource() {
		HikariConfig config = getInitialConfig();
		config.setJdbcUrl(url);
		config.setUsername("SA");
		config.setPassword("");
		HikariDataSource data = new HikariDataSource(config);
		data.setConnectionTimeout(25000L);
		return data;
	}

}
