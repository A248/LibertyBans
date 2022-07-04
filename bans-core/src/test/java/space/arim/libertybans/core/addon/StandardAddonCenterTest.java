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

package space.arim.libertybans.core.addon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class StandardAddonCenterTest {

	private SimpleAddon simpleAddon;
	private StandardAddonCenter addonCenter;
	private Path addonsFolder;

	@BeforeEach
	public void setup(@TempDir Path folder) {
		Set<Addon<?>> addons = new HashSet<>();
		addonCenter = new StandardAddonCenter(
				new IndifferentFactoryOfTheFuture(), folder, () -> Collections.unmodifiableSet(addons)
		);
		simpleAddon = new SimpleAddon(addonCenter);
		addons.add(simpleAddon);
		addonsFolder = folder.resolve("addons");
		try {
			Files.createDirectories(addonsFolder);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Test
	public void configNotAvailableBeforeStartup() {
		assertThrows(RuntimeException.class, simpleAddon::config);
	}

	private void writeCustomConfig(boolean enable, String message) {
		Path configPath = addonsFolder.resolve(simpleAddon.identifier() + ".yml");
		try (BufferedWriter writer = Files.newBufferedWriter(configPath,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append("enable: ").append(Boolean.toString(enable))
					.append('\n')
					.append("message: '").append(message).append('\'');
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Test
	public void startup() {
		addonCenter.startup();
		SimpleAddonConfig defaultConfig = assertDoesNotThrow(simpleAddon::config);
		assertTrue(defaultConfig.enable());
		assertEquals("hello", defaultConfig.message());
		assertTrue(simpleAddon.enabled);
	}

	@Test
	public void startupWithCustomConfig() {
		writeCustomConfig(true, "something else");
		addonCenter.startup();
		SimpleAddonConfig defaultConfig = assertDoesNotThrow(simpleAddon::config);
		assertTrue(defaultConfig.enable());
		assertEquals("something else", defaultConfig.message());
		assertTrue(simpleAddon.enabled);
	}

	@Test
	public void startupButDisabled() {
		writeCustomConfig(false, "something else");
		addonCenter.startup();
		assertFalse(simpleAddon.enabled, "Addon should not have started");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void reloadOrRestartCausesStartup(boolean isRestart) {
		writeCustomConfig(false, "something else");
		addonCenter.startup();
		assumeFalse(simpleAddon.enabled);
		writeCustomConfig(true, "now we are enabled");
		if (isRestart) {
			addonCenter.restart();
		} else {
			addonCenter.reloadAddons().join();
		}
		assertTrue(simpleAddon.enabled, "Addon should be started");
		assertEquals(1, simpleAddon.phaseChanges);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void reloadOrRestartCausesShutdown(boolean isRestart) {
		addonCenter.startup();
		assumeTrue(simpleAddon.enabled);
		writeCustomConfig(false, "now we are disabled");
		if (isRestart) {
			addonCenter.restart();
		} else {
			addonCenter.reloadAddons().join();
		}
		assertFalse(simpleAddon.enabled, "Addon should be shut down");
		assertEquals(2, simpleAddon.phaseChanges);
	}

	@Test
	public void shutdown() {
		addonCenter.startup();
		assumeTrue(simpleAddon.enabled);
		addonCenter.shutdown();
		assertFalse(simpleAddon.enabled);
		assertEquals(2, simpleAddon.phaseChanges);
	}

	@Test
	public void alreadyShutdown() {
		writeCustomConfig(false, "already shut down");
		addonCenter.startup();
		assumeFalse(simpleAddon.enabled);
		addonCenter.shutdown();

		assertFalse(simpleAddon.enabled, "Addon should be shut down");
		assertEquals(0, simpleAddon.phaseChanges);
	}

	@Test
	public void reloadAddonConfiguration() {
		addonCenter.startup();
		SimpleAddonConfig defaultConfig = simpleAddon.config();
		assertTrue(defaultConfig.enable());
		assertEquals("hello", defaultConfig.message());

		writeCustomConfig(false, "new message");
		addonCenter.reloadAddons().join();
		SimpleAddonConfig newConfig = simpleAddon.config();
		assertFalse(newConfig.enable());
		assertEquals("new message", newConfig.message());
	}

	@Test
	public void noUnnecessaryShutdownAndStartupDuringReload() {
		// This test is very important, because it ensures atomicity of reloads; see #159
		addonCenter.startup();
		assumeTrue(simpleAddon.enabled);
		addonCenter.reloadAddons().join(); // No startup() or shutdown() should be called here
		assertEquals(1, simpleAddon.phaseChanges);
	}

	private static final class SimpleAddon extends AbstractAddon<SimpleAddonConfig> {

		private volatile boolean enabled;
		private int phaseChanges;

		private SimpleAddon(AddonCenter addonCenter) {
			super(addonCenter);
		}

		@Override
		public void startup() {
			if (enabled) {
				throw new AssertionError("Cannot start up if already enabled");
			}
			phaseChanges += 1;
			enabled = true;
		}

		@Override
		public void shutdown() {
			if (!enabled) {
				throw new AssertionError("Cannot shut down if never enabled");
			}
			phaseChanges += 1;
			enabled = false;
		}

		@Override
		public Class<SimpleAddonConfig> configInterface() {
			return SimpleAddonConfig.class;
		}

		@Override
		public String identifier() {
			return "simple-addon";
		}
	}

	public interface SimpleAddonConfig extends AddonConfig {

		@ConfDefault.DefaultString("hello")
		String message();

	}
}
