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

package space.arim.libertybans.env.standalone.launcher;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.Platforms;
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
		var logger = Logger.getLogger(StandaloneApplication.class.getName());
		try {
			Class.forName("com.google.gson.Gson");
			Class.forName("org.yaml.snakeyaml.Yaml");
		} catch (ClassNotFoundException ignored) {
			logger.warning("The Gson and SnakeYaml dependencies must be present");
			return;
		}
		new StandaloneApplication(
				Path.of("libertybans"), new JulBootstrapLogger(logger)
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
		while ((command = console.readLine()) != null) {
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
				.platform(Platforms.standalone())
				.executor(ForkJoinPool.commonPool())
				.build();
		ClassLoader launchLoader = launcher.attemptLaunch().join();
		BaseFoundation base;
		try {
			base = new Instantiator("space.arim.libertybans.env.standalone.StandaloneLauncher", launchLoader)
					.invoke(Void.class, null, folder);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			logger.warn("Failed to launch LibertyBans", ex);
			return null;
		}
		base.startup();
		return base;
	}

}
