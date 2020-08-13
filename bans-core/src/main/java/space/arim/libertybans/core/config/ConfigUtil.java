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
package space.arim.libertybans.core.config;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.configure.ValueTransformer;

import space.arim.libertybans.core.config.Configs.AddressStrictness;

final class ConfigUtil {

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private ConfigUtil() {}
	
	static List<ValueTransformer> configTransformers() {
		ValueTransformer timeTransformer = (key, value) -> {
			if (key.equals("formatting.dates")) {
				DateTimeFormatter result = null;
				if (value instanceof String) {
					String timeF = (String) value;
					try {
						result = DateTimeFormatter.ofPattern(timeF);
					} catch (IllegalArgumentException ignored) {}
				}
				if (result == null) {
					//result = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm");
					logger.info("Config option formatting.dates invalid: {}", value);
				}
				return result;
			}
			return value;
		};
		ValueTransformer strictnessTransformer = (key, value) -> {
			if (key.equals("enforcement.address-strictness")) {
				AddressStrictness result = null;
				if (value instanceof String) {
					String addrS = (String) value;
					try {
						result = AddressStrictness.valueOf(addrS);
					} catch (IllegalArgumentException ignored) {}
				}
				if (result == null) {
					//result = AddressStrictness.NORMAL;
					logger.info("Config option enforcement.address-strictness invalid: {}", value);
				}
				return result;
			}
			return value;
		};
		return List.of(timeTransformer, strictnessTransformer);
	}
	
}

enum Translation {
	EN
}
