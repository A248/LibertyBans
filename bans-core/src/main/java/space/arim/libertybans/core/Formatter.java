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

import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;
import space.arim.uuidvault.api.UUIDVault;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.parser.SendableMessageParser;
import space.arim.api.chat.parser.SendableMessageParser.ColourMode;
import space.arim.api.chat.parser.SendableMessageParser.JsonMode;
import space.arim.api.chat.parser.StandardSendableMessageParser;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;

public class Formatter {

	private final LibertyBansCore core;
	
	private static final Entry<String, Long>[] timeUnits;
	private static final SendableMessageParser parser = new StandardSendableMessageParser();
	
	private static final long MARGIN_OF_INITIATION = 10; // seconds
	
	Formatter(LibertyBansCore core) {
		this.core = core;
	}
	
	public SendableMessage parseMessage(String rawMessage) {
		JsonMode jsonMode = (isJsonEnabled()) ? JsonMode.JSON_SK : JsonMode.NONE;
		return parser.parseMessage(rawMessage, ColourMode.ALL_COLOURS, jsonMode);
	}
	
	public boolean isJsonEnabled() {
		return core.getConfigs().getMessages().getBoolean("json.enable");
	}
	
	/**
	 * Gets the punishment message for a punishment
	 * 
	 * @param punishment the punishment
	 * @return the punishment message future
	 */
	public CentralisedFuture<SendableMessage> getPunishmentMessage(Punishment punishment) {
		return formatVictim(punishment.getVictim()).thenApplyAsync((victimFormatted) -> {
			String path = "additions." + punishment.getType().getLowercaseNamePlural() + ".layout";
			return formatAndParseAllLines(core.getConfigs().getMessages().getStringList(path), punishment, victimFormatted);
		});
	}
	
	private SendableMessage formatAndParseAllLines(List<String> messages, Punishment punishment, String victimFormatted) {
		StringBuilder result = new StringBuilder();
		String[] msgArray = messages.toArray(new String[] {});
		for (int n = 0; n < msgArray.length; n++) {
			if (n != 0) {
				result.append('\n');
			}
			result.append(msgArray[n]);
		}
		return formatWithPunishmentAsSendableMessage(result.toString(), punishment, victimFormatted);
	}
	
	private String formatWithPunishment0(String message, Punishment punishment, String victimFormatted) {
		long now = MiscUtil.currentTime();
		long start = punishment.getStart();
		long end = punishment.getEnd();

		long duration = end - start;
		long timePassed = now - start;
		long timeRemaining;
		if (timePassed < MARGIN_OF_INITIATION) {
			timeRemaining = duration;
		} else {
			timeRemaining = end - now;
		}
		return message.replace("%ID%", Integer.toString(punishment.getID()))
				.replace("%TYPE%", formatType(punishment.getType())).replace("%VICTIM%", victimFormatted)
				.replace("%VICTIM_ID%", formatVictimId(punishment.getVictim()))
				.replace("%OPERATOR%", formatOperator(punishment.getOperator()))
				.replace("%REASON%", punishment.getReason())
				.replace("%SCOPE%", core.getScopeManager().getServer(punishment.getScope()))
				.replace("%DURATION%", formatRelative(duration))
				.replace("%TIME_START_ABS%", formatAbsolute(start))
				.replace("%TIME_START_REL%", formatRelative(timePassed))
				.replace("%TIME_END_ABS%", formatAbsolute(end))
				.replace("%TIME_END_REL%", formatRelative(timeRemaining));
	}
	
	private SendableMessage formatWithPunishmentAsSendableMessage(String message, Punishment punishment, String victimFormatted) {
		return parseMessage(formatWithPunishment0(message, punishment, victimFormatted));
	}
	
	public CentralisedFuture<SendableMessage> formatWithPunishment(String message, Punishment punishment) {
		return formatVictim(punishment.getVictim()).thenApplyAsync((victimFormatted) -> {
			return formatWithPunishmentAsSendableMessage(message, punishment, victimFormatted);
		});
	}

	private String formatType(PunishmentType type) {
		return type.toString();
	}
	
	private String formatVictimId(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			return UUIDUtil.toShortString(((PlayerVictim) victim).getUUID());
		case ADDRESS:
			return formatAddressVictim((AddressVictim) victim);
		default:
			throw new IllegalStateException("Unknown victim type " + victim.getType());
		}
	}
	
	private CentralisedFuture<String> formatVictim(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			/*
			 * This should be a complete future every time we call this ourselves, because of UUIDMaster's fastCache.
			 * However, for API calls, the UUID/name might not be added to the cache.
			 */
			return core.getFuturesFactory().copyFuture(UUIDVault.get().resolve(((PlayerVictim) victim).getUUID()));
		case ADDRESS:
			return core.getFuturesFactory().completedFuture(formatAddressVictim((AddressVictim) victim));
		default:
			throw new IllegalStateException("Unknown victim type " + victim.getType());
		}
	}
	
	private String formatOperator(Operator operator) {
		switch (operator.getType()) {
		case CONSOLE:
			return core.getConfigs().getConfig().getString("formatting.console-display");
		case PLAYER:
			// This should never be null, because of UUIDMaster's fastCache
			return Objects.requireNonNull(UUIDVault.get().resolveImmediately(((PlayerOperator) operator).getUUID()));
		default:
			throw new IllegalStateException("Unknown operator type " + operator.getType());
		}
	}
	
	private String formatAddressVictim(AddressVictim addressVictim) {
		return addressVictim.getAddress().toString();
	}
	
	private String formatAbsolute(long time) {
		return core.getConfigs().getTimeFormatter().format(Instant.ofEpochSecond(time));
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
	
	private String formatRelative(long diff) {
		if (diff < 0) {
			return formatRelative(-diff);
		}
		List<String> result = new ArrayList<>();
		for (Entry<String, Long> unit : timeUnits) {
			String unitName = unit.getKey();
			long unitLength = unit.getValue();
			if (diff > unitLength && core.getConfigs().getMessages().getBoolean("misc.time." + unitName + ".enable")) {
				long amount = (diff / unitLength);
				diff -= (amount * unitLength);
				result.add(core.getConfigs().getMessages().getString("misc.time." + unitName + ".message").replace(unitName.toUpperCase(), Long.toString(amount)));
			}
		}
		StringBuilder builder = new StringBuilder();
		String[] resultArray = result.toArray(new String[] {});
	
		for (int n = 0; n < resultArray.length; n++) {
			if (n != 0) {
				builder.append(", ");
			}
			if (n == resultArray.length - 1) {
				builder.append(core.getConfigs().getMessages().getString("misc.time.and"));
			}
			builder.append(resultArray[n]);
		}
		return builder.toString();
	}
	
}
