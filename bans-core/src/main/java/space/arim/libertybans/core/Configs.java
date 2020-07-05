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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.universal.util.ThisClass;

import space.arim.uuidvault.api.UUIDVault;

import space.arim.api.util.config.Config;
import space.arim.api.util.config.SimpleConfig;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

public class Configs implements Part {

	private final LibertyBansCore core;
	
	private final Config config;
	private final Config messages;
	
	private volatile DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm");
	private volatile AddressStrictness addrStrictness = AddressStrictness.NORMAL;
	
	private static final Entry<String, Long>[] timeUnits;
	
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
	
	public String getPunishmentMessage(Punishment punishment) {
		String path;
		switch (punishment.getType()) {
		case BAN:
			path = "additions.bans.layout";
			break;
		case MUTE:
			path = "additions.mutes.layout";
			break;
		case WARN:
			path = "additions.warns.layout";
			break;
		case KICK:
			path = "additions.kicks.layout";
			break;
		default:
			throw new IllegalStateException("Unknown punishment type " + punishment.getType());
		}
		return formatAll(messages.getStringList(path), punishment);
	}
	
	private String formatAll(List<String> messages, Punishment punishment) {
		StringBuilder result = new StringBuilder();
		String[] msgArray = messages.toArray(new String[] {});
		for (int n = 0; n < msgArray.length; n++) {
			if (n != 0) {
				result.append('\n');
			}
			result.append(core.getConfigs().formatWithPunishment(msgArray[n], punishment));
		}
		return result.toString();
	}
	
	String formatWithPunishment(String message, Punishment punishment) {
		long now = System.currentTimeMillis();
		long start = punishment.getStart();
		long end = punishment.getEnd();
		return message.replace("%ID%", Integer.toString(punishment.getID())).replace("%TYPE%", formatType(punishment.getType())).replace("%VICTIM%", formatVictim(punishment.getVictim()))
				.replace("%OPERATOR%", formatOperator(punishment.getOperator())).replace("%REASON%", punishment.getReason())
				.replace("%SCOPE%", ((Scoper.ScopeImpl) punishment.getScope()).server).replace("%TIME_START_ABS%", formatAbsolute(start))
				.replace("%TIME_START_REL%", formatRelative(start, now)).replace("%TIME_END_ABS%", formatAbsolute(end))
				.replace("%TIME_END_REL%", formatRelative(end, now)).replace("%DURATION%", formatRelative(end, start));
	}

	String formatType(PunishmentType type) {
		return type.toString();
	}
	
	String formatVictim(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			// This should never be null, because of UUIDMaster's fastCache
			return UUIDVault.get().resolveImmediately(((PlayerVictim) victim).getUUID());
		case ADDRESS:
			return formatAddress(((AddressVictim) victim).getAddress());
		default:
			throw new IllegalStateException("Unknown victim type " + victim.getType());
		}
	}
	
	String formatOperator(Operator operator) {
		switch (operator.getType()) {
		case CONSOLE:
			return config.getString("formatting.console-display");
		case PLAYER:
			// This should never be null, because of UUIDMaster's fastCache
			return UUIDVault.get().resolveImmediately(((PlayerOperator) operator).getUUID());
		default:
			throw new IllegalStateException("Unknown operator type " + operator.getType());
		}
	}
	
	String formatAddress(byte[] address) {
		try {
			return InetAddress.getByAddress(address).getHostAddress();
		} catch (UnknownHostException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	private String formatAbsolute(long time) {
		return timeFormatter.format(Instant.ofEpochMilli(time));
	}
	
	static {
		List<Entry<String, Long>> list = List.of(timeUnitEntry("years", 31_536_000L),
				timeUnitEntry("months", 2_592_000L), timeUnitEntry("weeks", 604_800L), timeUnitEntry("days", 86_400L),
				timeUnitEntry("hours", 3_600L), timeUnitEntry("minutes", 60L));
		@SuppressWarnings("unchecked")
		Entry<String, Long>[] asArray = (Entry<String, Long>[]) list.toArray(new Map.Entry<?, ?>[] {});
		timeUnits = asArray;
	}
	
	private static Map.Entry<String, Long> timeUnitEntry(String key, long value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}
	
	private String formatRelative(long time, long current) {
		if (time < current) {
			return "-" + formatRelative(current, time);
		}
		long diff = time - current;
		List<String> result = new ArrayList<>();
		for (Entry<String, Long> unit : timeUnits) {
			String unitName = unit.getKey();
			long unitLength = unit.getValue();
			if (diff > unitLength && messages.getBoolean("time." + unitName + ".enable")) {
				long amount = (diff / unitLength);
				diff -= (amount * unitLength);
				result.add(messages.getString("time." + unitName + ".message").replace(unitName.toUpperCase(), Long.toString(amount)));
			}
		}
		StringBuilder builder = new StringBuilder();
		String[] resultArray = result.toArray(new String[] {});
	
		for (int n = 0; n < resultArray.length; n++) {
			if (n != 0) {
				builder.append(", ");
			}
			if (n == resultArray.length) {
				builder.append(messages.getString("time.and"));
			}
			builder.append(resultArray[n]);
		}
		return builder.toString();
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
