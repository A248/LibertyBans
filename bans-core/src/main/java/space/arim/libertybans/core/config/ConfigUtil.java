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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.parser.SendableMessageParser;
import space.arim.api.chat.parser.StandardSendableMessageParser;
import space.arim.api.chat.parser.SendableMessageParser.ColourMode;
import space.arim.api.chat.parser.SendableMessageParser.JsonMode;
import space.arim.api.configure.SingleKeyValueTransformer;
import space.arim.api.configure.ValueTransformer;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.uuid.UUIDMaster;

final class ConfigUtil {

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final SendableMessageParser parser = new StandardSendableMessageParser();
	
	private ConfigUtil() {}
	
	private static ValueTransformer dateFormatTransformer() {
		return SingleKeyValueTransformer.create("date-formatting.format", (value) -> {
			DateTimeFormatter result = null;
			if (value instanceof String) {
				String timeF = (String) value;
				try {
					result = DateTimeFormatter.ofPattern(timeF);
				} catch (IllegalArgumentException ignored) {}
			}
			if (result == null) {
				//result = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm");
				logger.info("Config option date-formatting.format invalid: {}", value);
			}
			return result;
		});
	}
	
	private static ValueTransformer dateZoneTransformer() {
		return SingleKeyValueTransformer.create("date-formatting.timezone", (value) -> {
			ZoneId result = null;
			if (value instanceof String) {
				String timeZ = (String) value;
				if (timeZ.equalsIgnoreCase("default")) {
					return ZoneId.systemDefault();
				}
				try {
					return ZoneId.of(timeZ);
				} catch (ZoneRulesException ex) {
					logger.warn("Unknown region ID {} (ZoneRulesException)", timeZ);
				} catch (DateTimeException ex) {
					logger.warn("Invalid timezone {} (DateTimeException)", timeZ);
				}
				return null;
			}
			logger.info("Config option date-formatting.timezone invalid: {}", value);
			return result;
		});
	}
	
	private static ValueTransformer strictnessTransformer() {
		return SingleKeyValueTransformer.create("enforcement.address-strictness", (value) -> {
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
		});
	}
	
	private static ValueTransformer syncEnforcementTransformer() {
		return SingleKeyValueTransformer.create("enforcement.sync-events-strategy", (value) -> {
			SyncEnforcement result = null;
			if (value instanceof String) {
				String syncE = (String) value;
				try {
					result = SyncEnforcement.valueOf(syncE);
				} catch (IllegalArgumentException ignored) {}
			}
			if (result == null) {
				//result = SyncEnforcement.ALLOW;
				logger.info("Config option enforcement.sync-events-strategy invalid: {}", value);
			}
			return result;
		});
	}
	
	private static ValueTransformer prefixTransformer() {
		return SingleKeyValueTransformer.create("all.prefix.value", (value) -> {
			if (value instanceof String) {
				return parseMessage(false, (String) value);
			}
			logger.info("Bad config value for all.prefix.value {}", value);
			return null;
		});
	}
	
	private static ValueTransformer combinedListStringTransformer(String key) {
		return SingleKeyValueTransformer.create(key, (value) -> {
			if (value instanceof List) {
				List<?> messages = (List<?>) value;

				StringBuilder result = new StringBuilder();
				for (int n = 0; n < messages.size(); n++) {
					if (n != 0) {
						result.append('\n');
					}
					Object element = messages.get(n);
					if (!(element instanceof String)) {
						logger.info("Bad list element for {} {}", key, element);
						return null;
					}
					result.append(element);
				}
				return result.toString();
			}
			logger.info("Bad config value for {} {}", key, value);
			return null;
		});
	}
	
	static List<ValueTransformer> configTransformers() {
		List<ValueTransformer> result = new ArrayList<>();
		result.add(dateFormatTransformer());
		result.add(dateZoneTransformer());
		result.add(strictnessTransformer());
		result.add(syncEnforcementTransformer());
		result.addAll(UUIDMaster.createValueTransformers());
		return result;
	}
	
	static List<ValueTransformer> messagesTransformers() {
		List<ValueTransformer> result = new ArrayList<>();
		result.add(prefixTransformer());
		result.add(combinedListStringTransformer("all.usage"));
		for (PunishmentType type : MiscUtil.punishmentTypes()) {
			result.add(combinedListStringTransformer("additions." + type.getLowercaseNamePlural() + ".layout"));
		}
		return result;
	}
	
	static SendableMessage parseMessage(boolean useJson, String rawMessage) {
		JsonMode jsonMode = (useJson) ? JsonMode.JSON_SK : JsonMode.NONE;
		return parser.parseMessage(rawMessage, ColourMode.ALL_COLOURS, jsonMode);
	}
	
}

enum Translation {
	EN
}
