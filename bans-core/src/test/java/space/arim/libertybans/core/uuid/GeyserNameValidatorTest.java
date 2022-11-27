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

package space.arim.libertybans.core.uuid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import space.arim.omnibus.util.UUIDUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeyserNameValidatorTest {

	private NameValidator validator;

	@BeforeEach
	public void setup() {
		validator = StandardNameValidator.createFromPrefix(".");
	}

	@ParameterizedTest
	@ValueSource(strings = {"A248", "ObsidianWolf_", "lol_haha_dead", "123Numbers"})
	public void validJavaEditionNames(String name) {
		assertTrue(validator.validateNameArgument(name));
		assertTrue(validator.isVanillaName(name));
	}

	@ParameterizedTest
	@ValueSource(strings = {".BedrockPlayer", ".123Numbers"})
	public void validBedrockEditionNames(String name) {
		assertTrue(validator.validateNameArgument(name));
		assertFalse(validator.isVanillaName(name));
	}

	@Test
	public void nonAlphaNumericUnderscore() {
		assertFalse(validator.validateNameArgument("NameWith=Sign"));
	}

	@Test
	public void geyserPrefixNotUsedAsPrefix() {
		assertFalse(validator.validateNameArgument("NameWith."));
	}

	@Test
	public void tooManyCharacters() {
		assertFalse(validator.validateNameArgument("ThisNameHasMoreThan16Characters"));
	}

	@Test
	public void validateUUIDs() {
		assertTrue(validator.isVanillaUUID(UUIDUtil.fromShortString("ed5f12cd600745d9a4b9940524ddaecf")));
		assertTrue(validator.isVanillaUUID(UUID.fromString("0b58c22d-56f5-3296-87b8-c0155a071d4d")), "Offline UUID");
		assertFalse(validator.isVanillaUUID(UUID.fromString("00000000-0000-0000-0009-01f64f65c7c3")));
	}

}
