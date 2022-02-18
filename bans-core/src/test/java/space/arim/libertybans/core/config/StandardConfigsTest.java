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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class StandardConfigsTest {

	@TempDir
	public Path folder;

	private StandardConfigs configs;

	@BeforeEach
	public void setup() {
		configs = new StandardConfigs(folder);
	}

	@Test
	public void loadDefaults() {
		assertTrue(configs.reloadConfigs().join());
	}

	@Test
	public void loadAndReloadDefaults() {
		assumeTrue(configs.reloadConfigs().join());
		assertTrue(configs.reloadConfigs().join());
	}

	@ParameterizedTest
	@EnumSource
	public void copyAndValidateTranslations(Translation translation) throws IOException {
		Path langFolder = folder.resolve("lang");
		Files.createDirectories(langFolder);
		configs.createLangFiles(langFolder).join();

		Path langFile = langFolder.resolve("messages_" + translation.name().toLowerCase(Locale.ROOT) + ".yml");
		assertTrue(Files.exists(langFile));

		ConfigHolder<MessagesConfig> configHolder = new ConfigHolder<>(MessagesConfig.class);
		assertEquals(
				ConfigResult.SUCCESS_LOADED,
				configHolder.reload(langFile).join()
		);
	}
}
