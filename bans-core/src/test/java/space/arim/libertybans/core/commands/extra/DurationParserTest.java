/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.commands.extra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import space.arim.libertybans.core.config.ParsedDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DurationParserTest {

	private DurationParser parser;

	@BeforeEach
	public void setup() {
		parser = new DurationParser();
	}

	@TestFactory
	public Stream<DynamicNode> parseCorrect() {
		return Stream.of(
				new ParsedDuration("1m", Duration.ofMinutes(1L)),
				new ParsedDuration("5h", Duration.ofHours(5L)),
				new ParsedDuration("5H", Duration.ofHours(5L)),
				new ParsedDuration("30d", Duration.ofDays(30L)),
				new ParsedDuration("1MO", ChronoUnit.MONTHS.getDuration()),
				new ParsedDuration("2y", ChronoUnit.YEARS.getDuration().multipliedBy(2)),
				new ParsedDuration("perm", Duration.ZERO),
				new ParsedDuration("Perm", Duration.ZERO)
		).map(this::testCorrect);
	}

	private DynamicNode testCorrect(ParsedDuration durationPermission) {
		return DynamicTest.dynamicTest("Verifying correct " + durationPermission, () -> {
			assertEquals(durationPermission.duration(), parser.parse(durationPermission.value()),
					() -> "Duration " + durationPermission + " should parse correctly");
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"unparsable", "1", "d", "mo", "1p", "any other special case that needs testing?"})
	public void parseInvalid(String argument) {
		Duration duration = parser.parse(argument);
		assertTrue(duration.isNegative(), () -> "Should have failed to parse " + argument);
	}

}
