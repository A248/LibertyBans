/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.env.standalone.launcher;

import space.arim.libertybans.bootstrap.*;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;
import space.arim.libertybans.bootstrap.logger.JulBootstrapLogger;

import java.io.Console;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class StandaloneApplication {

	private final Path folder;
	private final BootstrapLogger logger;

	public StandaloneApplication(Path folder, BootstrapLogger logger) {
		this.folder = folder;
		this.logger = logger;
	}

	public static void main(String[] args) {
		new StandaloneApplication(
				Path.of("libertybans"),
				new JulBootstrapLogger(Logger.getLogger(StandaloneApplication.class.getName()))
		).appStart();
	}

	private void appStart() {
		logger.info("Starting the standalone instance. Use command 'stop' to stop.");
		BaseFoundation base = initialize();
		if (base == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		Consumer<String> commandDispatch = (Consumer<String>) base.platformAccess();

		Console console = System.console();
		String command;
		while ((command = console.readLine("> ")) != null) {
			if (command.equalsIgnoreCase("stop")) {
				break;
			}
			commandDispatch.accept(command);
		}
		base.shutdown();
	}

	private BaseFoundation initialize() {
		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(folder)
				.logger(logger)
				.platform(Platform
						.builder(Platform.Category.STANDALONE)
						.nameAndVersion("JVM", Runtime.version().toString()))
				.executor(ForkJoinPool.commonPool())
				.build();
		Payload<Object> payload = launcher.getPayload(Payload.NO_PLUGIN);
		ClassLoader launchLoader = launcher.attemptLaunch().join();
		BaseFoundation base;
		try {
			base = new Instantiator(
					"space.arim.libertybans.env.standalone.StandaloneLauncher", launchLoader
			).invoke(payload);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			logger.warn("Failed to launch LibertyBans", ex);
			return null;
		}
		base.startup();
		return base;
	}

}
