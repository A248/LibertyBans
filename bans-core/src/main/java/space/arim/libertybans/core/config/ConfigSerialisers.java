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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import space.arim.api.jsonchat.adventure.ChatMessageComponentSerializer;

import space.arim.api.util.dazzleconf.ComponentTextSerializer;
import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;
import space.arim.libertybans.core.scope.ConfiguredScope;

final class ConfigSerialisers {

	private ConfigSerialisers() {}

	static void addTo(ConfigurationOptions.Builder builder) {
		builder.addSerialisers(
				new ComponentValueSerializer(new ChatMessageComponentSerializer()),
				new ComponentTextSerializer(),
				new ParsedDuration.Serializer(),
				new DateTimeFormatterSerialiser(),
				new ZoneIdSerialiser(),
				new ConfiguredScope.Serializer()
		);
	}

	private static class ComponentValueSerializer implements ValueSerialiser<Component> {

		private final ComponentSerializer<Component, Component, String> adventureSerializer;

		ComponentValueSerializer(ComponentSerializer<Component, Component, String> adventureSerializer) {
			this.adventureSerializer = Objects.requireNonNull(adventureSerializer);
		}

		@Override
		public Class<Component> getTargetClass() {
			return Component.class;
		}

		@Override
		public Component deserialise(FlexibleType flexibleType) throws BadValueException {
			List<String> lines = flexibleType.getList(FlexibleType::getString);
			List<Component> components = new ArrayList<>(2 * lines.size());
			for (String line : lines) {
				if (!components.isEmpty()) {
					components.add(Component.newline());
				}
				components.add(adventureSerializer.deserialize(line));
			}
			if (components.size() == 1) {
				return components.get(0);
			}
			return TextComponent.ofChildren(components.toArray(Component[]::new));
		}

		@Override
		public Object serialise(Component value, Decomposer decomposer) {
			String content = adventureSerializer.serialize(value);
			List<String> lines = content.lines().collect(Collectors.toList());
			if (lines.size() == 1) {
				return lines.get(0);
			}
			return lines;
		}
		
	}
	
	private static class DateTimeFormatterSerialiser implements ValueSerialiser<DateTimeFormatterWithPattern> {

		@Override
		public Class<DateTimeFormatterWithPattern> getTargetClass() {
			return DateTimeFormatterWithPattern.class;
		}

		@Override
		public DateTimeFormatterWithPattern deserialise(FlexibleType flexibleType) throws BadValueException {
			String format = flexibleType.getString();
			try {
				return new DateTimeFormatterWithPattern(format);
			} catch (IllegalArgumentException ex) {
				throw flexibleType.badValueExceptionBuilder().message("Bad date format " + format).cause(ex).build();
			}
		}

		@Override
		public String serialise(DateTimeFormatterWithPattern value, Decomposer decomposer) {
			return value.getPattern();
		}
		
	}
	
	private static class ZoneIdSerialiser implements ValueSerialiser<ZoneId> {

		@Override
		public Class<ZoneId> getTargetClass() {
			return ZoneId.class;
		}

		@Override
		public ZoneId deserialise(FlexibleType flexibleType) throws BadValueException {
			String zone = flexibleType.getString();
			if (zone.equalsIgnoreCase("default")) {
				return ZoneId.systemDefault();
			}
			try {
				return ZoneId.of(zone);
			} catch (ZoneRulesException ex) {
				throw flexibleType.badValueExceptionBuilder().message(
						"Unknown region ID in " + zone + " (ZoneRulesException)").cause(ex).build();
			} catch (DateTimeException ex) {
				throw flexibleType.badValueExceptionBuilder().message(
						"Invalid timezone in " + zone + " (DateTimeException)").cause(ex).build();
			}
		}

		@Override
		public Object serialise(ZoneId value, Decomposer decomposer) {
			return value.getId();
		}

	}
	
}
