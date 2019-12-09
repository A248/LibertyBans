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

public class RemoteSettings extends SqlSettings {
	
	private final String url;
	private final String username;
	private final String password;
	
	public RemoteSettings(ConfigMaster config) {
		super(config, "mysql");
		url = config.getConfigString("storage.mysql.url").replace("%HOST%", config.getConfigString("storage.mysql.host")).replace("%PORT%", Integer.toString(config.getConfigInt("storage.mysql.port"))).replaceAll("%DATABASE%", config.getConfigString("storage.mysql.database"));
		username = config.getConfigString("storage.mysql.username");
		password = config.getConfigString("storage.mysql.password");
	}
	
	@Override
	Class<?> getDriverClass() {
		return com.mysql.jdbc.Driver.class;
	}

	@Override
	HikariDataSource loadDataSource() {
		HikariConfig config = getInitialConfig();
		config.setJdbcUrl(url);
		config.setUsername(username);
		config.setPassword(password);
		config.setConnectionTimeout(25000L);
		return new HikariDataSource(config);
	}
	
}
