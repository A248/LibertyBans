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
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.stream.Collectors;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;
import space.arim.api.chat.serialiser.JsonSkSerialiser;

import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;

final class ConfigSerialisers {
	
	private ConfigSerialisers() {}
	
	static void addTo(ConfigurationOptions.Builder builder) {
		List<ValueSerialiser<?>> serialisers = List.of(new MessageManipulatorSerialiser(), new MessageSerialiser(),
				new DateTimeFormatterSerialiser(), new ZoneIdSerialiser());
		for (ValueSerialiser<?> serialiser : serialisers) {
			builder.addSerialiser(serialiser);
		}
	}

	private static class MessageManipulatorSerialiser implements ValueSerialiser<SendableMessageManipulator> {

		@Override
		public Class<SendableMessageManipulator> getTargetClass() {
			return SendableMessageManipulator.class;
		}

		@Override
		public SendableMessageManipulator deserialise(FlexibleType flexibleType) throws BadValueException {
			return SendableMessageManipulator.create(flexibleType.getObject(SendableMessage.class));
		}

		@Override
		public Object serialise(SendableMessageManipulator value, Decomposer decomposer) {
			return decomposer.decompose(SendableMessage.class, value.getMessage());
		}
		
	}
	
	private static class MessageSerialiser implements ValueSerialiser<SendableMessage> {
		
		@Override
		public Class<SendableMessage> getTargetClass() {
			return SendableMessage.class;
		}

		@Override
		public SendableMessage deserialise(FlexibleType flexibleType) throws BadValueException {
			String content = String.join("\n", flexibleType.getList((flexibleElement) -> flexibleElement.getString()));
			return JsonSkSerialiser.getInstance().deserialise(content);
		}

		@Override
		public Object serialise(SendableMessage value, Decomposer decomposer) {
			String content = JsonSkSerialiser.getInstance().serialise(value);
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
