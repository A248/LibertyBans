/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.config;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import space.arim.api.jsonchat.adventure.ChatMessageComponentSerializer;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class Formatter implements InternalFormatter {

	private final FactoryOfTheFuture futuresFactory;
	private final Configs configs;
	private final InternalScopeManager scopeManager;
	private final UUIDManager uuidManager;
	private final Time time;
	private final ComponentSerializer<Component, ? extends Component, String> messageParser;

	private static final long MARGIN_OF_INITIATION = 10; // seconds
	
	@Inject
	public Formatter(FactoryOfTheFuture futuresFactory, Configs configs, InternalScopeManager scopeManager,
					 UUIDManager uuidManager, Time time) {
		this(futuresFactory, configs, scopeManager, uuidManager, time, new ChatMessageComponentSerializer());
	}

	Formatter(FactoryOfTheFuture futuresFactory, Configs configs, InternalScopeManager scopeManager,
			  UUIDManager uuidManager, Time time, ComponentSerializer<Component, ? extends Component, String> messageParser) {
		this.futuresFactory = futuresFactory;
		this.configs = configs;
		this.scopeManager = scopeManager;
		this.uuidManager = uuidManager;
		this.time = time;
		this.messageParser = messageParser;
	}
	
	private MessagesConfig messages() {
		return configs.getMessagesConfig();
	}
	
	@Override
	public Component parseMessageWithoutPrefix(String messageToParse) {
		return messageParser.deserialize(messageToParse);
	}
	
	@Override
	public ComponentLike prefix(ComponentLike message) {
		Component prefix = configs.getMessagesConfig().all().prefix();
		if (prefix instanceof TextComponent && ((TextComponent) prefix).content().isEmpty() && prefix.children().isEmpty()) {
			// Empty prefix optimization
			return message;
		}
		return TextComponent.ofChildren(prefix, message);
	}
	
	@Override
	public CentralisedFuture<Component> getPunishmentMessage(Punishment punishment) {
		return formatWithPunishment(messages().additions().forType(punishment.getType()).layout(), punishment);
	}
	
	@Override
	public CentralisedFuture<Component> formatWithPunishment(ComponentText componentText,
															 Punishment punishment) {
		return formatWithPunishment(componentText, punishment, null);
	}
	
	@Override
	public CentralisedFuture<Component> formatWithPunishmentAndUnoperator(ComponentText componentText,
																		  Punishment punishment, Operator unOperator) {
		return formatWithPunishment(componentText, punishment, Objects.requireNonNull(unOperator, "unOperator"));
	}

	private CentralisedFuture<Component> formatWithPunishment(ComponentText componentText,
															  Punishment punishment, Operator unOperator) {
		Map<FutureReplaceable, CentralisedFuture<String>> futureReplacements = new EnumMap<>(FutureReplaceable.class);
		for (FutureReplaceable futureReplaceable : FutureReplaceable.values()) {

			if (unOperator == null && futureReplaceable == FutureReplaceable.UNOPERATOR) {
				continue;
			}
			if (componentText.contains(futureReplaceable.getVariable())) {
				CentralisedFuture<String> replacement = getFutureReplacement(futureReplaceable, punishment, unOperator);
				futureReplacements.put(futureReplaceable, replacement);
			}
		}
		return futuresFactory.supplyAsync(() -> getSimpleReplacements(punishment, unOperator))
				.thenCompose((simpleReplacements) -> {
			return formatWithPunishment0(componentText, simpleReplacements, futureReplacements);
		});
	}
	
	private enum SimpleReplaceable {
		ID,
		TYPE,
		TYPE_VERB,
		VICTIM_ID,
		OPERATOR_ID,
		UNOPERATOR_ID,
		REASON,
		SCOPE,
		DURATION,
		START_DATE,
		TIME_PASSED,
		TIME_PASSED_SIMPLE,
		END_DATE,
		TIME_REMAINING,
		TIME_REMAINING_SIMPLE,
		HAS_EXPIRED,
		TRACK,
		TRACK_ID,
		TRACK_NAMESPACE,
		;

		String getVariable() {
			return "%" + name() + "%";
		}
		
	}
	
	private enum FutureReplaceable {
		VICTIM,
		OPERATOR,
		UNOPERATOR;

		String getVariable() {
			return "%" + name() + "%";
		}
	}
	
	private Map<SimpleReplaceable, String> getSimpleReplacements(Punishment punishment, Operator unOperator) {
		MessagesConfig.Formatting formatting = messages().formatting();

		Map<SimpleReplaceable, String> simpleReplacements = new EnumMap<>(SimpleReplaceable.class);
		simpleReplacements.put(SimpleReplaceable.ID, Long.toString(punishment.getIdentifier()));
		simpleReplacements.put(SimpleReplaceable.TYPE, formatPunishmentType(punishment.getType()));
		simpleReplacements.put(SimpleReplaceable.TYPE_VERB, formatPunishmentTypeVerb(punishment.getType()));
		simpleReplacements.put(SimpleReplaceable.VICTIM_ID, formatVictimId(punishment.getVictim()));
		simpleReplacements.put(SimpleReplaceable.OPERATOR_ID, formatOperatorId(punishment.getOperator()));
		if (unOperator != null)
			simpleReplacements.put(SimpleReplaceable.UNOPERATOR_ID, formatOperatorId(unOperator));
		simpleReplacements.put(SimpleReplaceable.REASON, punishment.getReason());
		simpleReplacements.put(SimpleReplaceable.SCOPE, formatScope(punishment.getScope()));
		{
			final long now = time.currentTime();
			final long start = punishment.getStartDateSeconds();

			final long timePassed = now - start;

			final String durationFormatted;
			final String relativeEndFormatted, relativeEndFormattedSimple;
			boolean notExpired = false;

			if (punishment.isPermanent()) {
				// Permanent punishment
				MessagesConfig.Formatting.PermanentDisplay display = formatting.permanentDisplay();
				durationFormatted = display.duration();
				relativeEndFormatted = display.relative();
				relativeEndFormattedSimple = relativeEndFormatted;
				notExpired = true;

			} else {
				final long end = punishment.getEndDateSeconds();
				assert end != 0 : end;
				// Temporary punishment
				long duration = end - start;
				durationFormatted = formatRelative(duration);

				if (timePassed < MARGIN_OF_INITIATION) {
					// Punishment recently enacted
					// Using a margin of initiation prevents the "29 days, 23 hours, 59 minutes" issue
					relativeEndFormatted = durationFormatted;
					relativeEndFormattedSimple = formatRelativeSimple(duration);
					notExpired = true;

				} else if (timePassed >= duration) {
					// Expired punishment
					relativeEndFormatted = formatting.noTimeRemainingDisplay();
					relativeEndFormattedSimple = relativeEndFormatted;
				} else {
					// Punishment still active
					long timeRemaining = end - now;
					relativeEndFormatted = formatRelative(timeRemaining);
					relativeEndFormattedSimple = formatRelativeSimple(timeRemaining);
					notExpired = true;
				}
			}
			simpleReplacements.put(SimpleReplaceable.DURATION, durationFormatted);
			simpleReplacements.put(SimpleReplaceable.START_DATE, formatAbsoluteDate(punishment.getStartDate()));
			simpleReplacements.put(SimpleReplaceable.TIME_PASSED, formatRelative(timePassed));
			simpleReplacements.put(SimpleReplaceable.TIME_PASSED_SIMPLE, formatRelativeSimple(timePassed));
			simpleReplacements.put(SimpleReplaceable.END_DATE, formatAbsoluteDate(punishment.getEndDate()));
			simpleReplacements.put(SimpleReplaceable.TIME_REMAINING, relativeEndFormatted);
			simpleReplacements.put(SimpleReplaceable.TIME_REMAINING_SIMPLE, relativeEndFormattedSimple);
			MessagesConfig.Formatting.PunishmentExpiredDisplay display = formatting.punishmentExpiredDisplay();
			simpleReplacements.put(SimpleReplaceable.HAS_EXPIRED, (notExpired) ? display.notExpired() : display.expired());
		}
		String track, trackId, trackNamespace;
		MessagesConfig.Formatting.TrackDisplay trackDisplay = formatting.trackDisplay();
		EscalationTrack escalationTrack = punishment.getEscalationTrack().orElse(null);
		if (escalationTrack == null) {
			track = trackDisplay.noTrack();
			trackId = trackDisplay.noTrackId();
			trackNamespace = trackDisplay.noTrackNamespace();
		} else {
			String id = escalationTrack.getValue();
			track = trackDisplay.trackDisplayNames().getOrDefault(id, id);
			trackId = id;
			trackNamespace = escalationTrack.getNamespace();
		}
		simpleReplacements.put(SimpleReplaceable.TRACK, track);
		simpleReplacements.put(SimpleReplaceable.TRACK_ID, trackId);
		simpleReplacements.put(SimpleReplaceable.TRACK_NAMESPACE, trackNamespace);

		return simpleReplacements;
	}

	private CentralisedFuture<Component> formatWithPunishment0(ComponentText componentText,
															   Map<SimpleReplaceable, String> simpleReplacements,
															   Map<FutureReplaceable, CentralisedFuture<String>> futureReplacements) {
		return futuresFactory.allOf(futureReplacements.values()).thenApply((ignore) -> {

			class Replacer implements UnaryOperator<String> {
				@Override
				public String apply(String text) {
					for (Map.Entry<SimpleReplaceable, String> simpleReplacement : simpleReplacements.entrySet()) {
						text = text.replace(
								simpleReplacement.getKey().getVariable(),
								simpleReplacement.getValue());
					}
					for (Map.Entry<FutureReplaceable, CentralisedFuture<String>> futureReplacement : futureReplacements.entrySet()) {
						text = text.replace(
								futureReplacement.getKey().getVariable(),
								futureReplacement.getValue().join());
					}
					return text;
				}
			}
			return componentText.replaceText(new Replacer()).asComponent();
		});
	}
	
	private CentralisedFuture<String> getFutureReplacement(FutureReplaceable futureReplaceable, Punishment punishment,
			Operator unOperator) {
		return switch (futureReplaceable) {
			case VICTIM -> formatVictim(punishment.getVictim());
			case OPERATOR -> formatOperator(punishment.getOperator());
			case UNOPERATOR -> formatOperator(unOperator);
		};
	}

	private String formatVictimId(Victim victim) {
		UUID uuid;
		if (victim instanceof PlayerVictim playerVictim) {
			uuid = playerVictim.getUUID();
		} else if (victim instanceof AddressVictim addressVictim) {
			return formatAddressVictim(addressVictim);
		} else if (victim instanceof CompositeVictim compositeVictim) {
			uuid = compositeVictim.getUUID();
		} else {
			throw MiscUtil.unknownVictimType(victim.getType());
		}
		return UUIDUtil.toShortString(uuid);
	}

	@Override
	public CentralisedFuture<String> formatVictim(Victim victim) {
		UUID uuid;
		if (victim instanceof PlayerVictim playerVictim) {
			uuid = playerVictim.getUUID();
		} else if (victim instanceof AddressVictim addressVictim) {
			return futuresFactory.completedFuture(formatAddressVictim(addressVictim));
		} else if (victim instanceof CompositeVictim compositeVictim) {
			uuid = compositeVictim.getUUID();
		} else {
			throw MiscUtil.unknownVictimType(victim.getType());
		}
		/*
		 * This should be a complete future every time we call this ourselves, because of UUIDManager's fastCache.
		 * However, for API calls, the uuid/name might not be added to the cache.
		 */
		return uuidManager.lookupName(uuid)
				.thenApply((optName) -> optName.orElse(
						messages().formatting().victimDisplay().playerNameUnknown()
				));
	}

	private String formatOperatorId(Operator operator) {
		if (operator instanceof PlayerOperator playerOperator) {
			return UUIDUtil.toShortString(playerOperator.getUUID());
		}
		return messages().formatting().consoleDisplay();
	}

	@Override
	public CentralisedFuture<String> formatOperator(Operator operator) {
		if (operator instanceof PlayerOperator playerOperator) {
			/*
			 * Similarly in #formatVictim, this should be a complete future every time we call this ourselves,
			 * because of UUIDMaster's fastCache.
			 */
			return uuidManager.lookupName(playerOperator.getUUID())
					.thenApply((optName) -> optName.orElse(
							messages().formatting().victimDisplay().playerNameUnknown()
					));
		}
		return futuresFactory.completedFuture(messages().formatting().consoleDisplay());
	}

	private String formatAddressVictim(AddressVictim addressVictim) {
		return addressVictim.getAddress().toString();
	}

	/** Visible for testing */
	String formatRelative(long diff) {
		if (diff < 0) {
			return formatRelative(-diff);
		}
		MessagesConfig.Misc.Time timeConfig = messages().misc().time();

		List<Map.Entry<ChronoUnit, String>> fragments = new ArrayList<>(timeConfig.fragments().entrySet());
		fragments.sort(Map.Entry.<ChronoUnit, String>comparingByKey().reversed());
		List<String> segments = new ArrayList<>(fragments.size());

		for (Map.Entry<ChronoUnit, String> fragment : fragments) {
			long unitLength = fragment.getKey().getDuration().toSeconds();
			if (diff >= unitLength) {
				long amount = (diff / unitLength);
				diff -= (amount * unitLength);
				segments.add(fragment.getValue().replace("%VALUE%", Long.toString(amount)));
			}
		}
		if (segments.isEmpty()) {
			return timeConfig.fallbackSeconds().replace("%VALUE%", Long.toString(diff));
		}
		if (segments.size() == 1) {
			return segments.get(0);
		}

		String delimiter = (timeConfig.useComma()) ? ", " : " ";
		StringJoiner joiner = new StringJoiner(delimiter);
		for (int n = 0; n < segments.size(); n++) {
			String segment = segments.get(n);
			boolean lastElement = n == segments.size() - 1;
			joiner.add((lastElement) ? timeConfig.and() + segment : segment);
		}
		return joiner.toString();
	}

	String formatRelativeSimple(long diff) {
		if (diff < 0) {
			return formatRelativeSimple(-diff);
		}
		MessagesConfig.Misc.Time timeConfig = messages().misc().time();

		List<Map.Entry<ChronoUnit, String>> fragments = new ArrayList<>(timeConfig.fragments().entrySet());
		fragments.sort(Map.Entry.<ChronoUnit, String>comparingByKey().reversed());
		String segment = "";

		for (Map.Entry<ChronoUnit, String> fragment : fragments) {
			long unitLength = fragment.getKey().getDuration().toSeconds();
			if (diff >= unitLength) {
				long amount = Math.round(diff / (double) unitLength);
				if (amount > 0) {
					segment = fragment.getValue().replace("%VALUE%", Long.toString(amount));
					break;
				}
			}
		}
		if (segment.isEmpty()) {
			return timeConfig.fallbackSeconds().replace("%VALUE%", Long.toString(diff));
		}
		return segment;
	}
	
	@Override
	public String formatPunishmentType(PunishmentType type) {
		String formatted = messages().formatting().punishmentTypeDisplay().get(type);
		return (formatted == null) ? type.toString() : formatted;
	}

	@Override
	public String formatPunishmentTypeVerb(PunishmentType type) {
		String formatted = messages().formatting().punishmentTypeVerbDisplay().get(type);
		return (formatted == null) ? type.toString() + "ed" : formatted;
	}

	@Override
	public ZoneId getTimezone() {
		return configs.getMainConfig().dateFormatting().zoneId();
	}
	
	@Override
	public DateTimeFormatter getDateTimeFormatter() {
		return configs.getMainConfig().dateFormatting().formatAndPattern().getFormatter();
	}
	
	@Override
	public String formatAbsoluteDate(Instant date) {
		if (date.equals(Instant.MAX)) { // implicit null check
			return messages().formatting().permanentDisplay().absolute();
		}
		return getDateTimeFormatter().format(date.atZone(getTimezone()));
	}

	@Override
	public String formatDuration(Duration duration) {
		if (duration.equals(Duration.ZERO)) { // implicit null check
			return messages().formatting().permanentDisplay().duration();
		}
		return formatRelative(duration.toSeconds());
	}
	
	@Override
	public String formatScope(ServerScope scope) {
		Objects.requireNonNull(scope, "scope");
		String globalScopeDisplay = messages().formatting().globalScopeDisplay();
		return scopeManager.display(scope, globalScopeDisplay);
	}
	
}
