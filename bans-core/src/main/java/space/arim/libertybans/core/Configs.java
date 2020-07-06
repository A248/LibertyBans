/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core;

import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.universal.util.ThisClass;

import space.arim.api.util.config.Config;
import space.arim.api.util.config.SimpleConfig;

public class Configs implements Part {

	private final LibertyBansCore core;
	
	private final Config config;
	private final Config messages;
	
	private volatile DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm");
	private volatile AddressStrictness addrStrictness = AddressStrictness.NORMAL;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	Configs(LibertyBansCore core) {
		this.core = core;
		config = new SimpleConfig(core.getFolder(), "config.yml");
		messages = new SimpleConfig(core.getFolder(), "messages.yml");
	}
	
	@Override
	public void startup() {
		config.saveDefaultConfig();
		config.reloadConfig();
		messages.saveDefaultConfig();
		messages.reloadConfig();
		reparseCached();
	}
	
	public Config getConfig() {
		return config;
	}
	
	public Config getMessages() {
		return messages;
	}
	
	public void reload() {
		config.reloadConfig();
		messages.reloadConfig();
		reparseCached();
	}
	
	private void reparseCached() {
		String addrS = config.getString("enforcement.address-strictness").toUpperCase();
		try {
			AddressStrictness newValue = AddressStrictness.valueOf(addrS);
			addrStrictness = newValue;
		} catch (IllegalArgumentException userMistake) {
			logger.info("Config option enforcement.address-strictness invalid: {}", addrS);
		}
		String timeF = config.getString("formatting.dates");
		try {
			DateTimeFormatter newValue = DateTimeFormatter.ofPattern(timeF);
			timeFormatter = newValue;
		} catch (IllegalArgumentException userMistake) {
			logger.info("Config option formatting.dates invalid: {}", timeF);
		}
	}
	
	DateTimeFormatter getTimeFormatter() {
		return timeFormatter;
	}

	public boolean strictAddressQueries() {
		return addrStrictness != AddressStrictness.LENIENT;
	}
	
	public enum AddressStrictness {
		LENIENT,
		NORMAL,
		STRICT
	}

	@Override
	public void shutdown() {
	}
	
}
