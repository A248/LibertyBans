/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.env.CmdSender;

import java.time.Duration;
import java.util.Objects;

public final class ParsedDuration {

	private transient final String value;
	private final Duration duration;

	public ParsedDuration(String value, Duration duration) {
		if (duration.isNegative()) { // implicit null check
			throw new IllegalArgumentException("Duration cannot be negative");
		}
		this.value = Objects.requireNonNull(value);
		this.duration = duration;
	}

	public String value() {
		return value;
	}

	public boolean hasDurationPermission(CmdSender sender, PunishmentType type) {
		return sender.hasPermission("libertybans." + type + ".dur." + value);
	}

	public Duration duration() {
		return duration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ParsedDuration that = (ParsedDuration) o;
		return duration.equals(that.duration);
	}

	@Override
	public int hashCode() {
		return duration.hashCode();
	}

	@Override
	public String toString() {
		return "ParsedDuration{" +
				"value='" + value + '\'' +
				", duration=" + duration +
				'}';
	}

	static final class Serializer implements ValueSerialiser<ParsedDuration> {

		@Override
		public Class<ParsedDuration> getTargetClass() {
			return ParsedDuration.class;
		}

		@Override
		public ParsedDuration deserialise(FlexibleType flexibleType) throws BadValueException {
			String value = flexibleType.getString();
			Duration duration = new DurationParser().parse(value);
			if (duration.isNegative()) {
				throw flexibleType.badValueExceptionBuilder().message(value + " must be a valid duration").build();
			}
			return new ParsedDuration(value, duration);
		}

		@Override
		public Object serialise(ParsedDuration value, Decomposer decomposer) {
			return value.value();
		}
	}
}
