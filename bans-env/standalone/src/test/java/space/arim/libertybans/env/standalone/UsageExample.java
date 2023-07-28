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

package space.arim.libertybans.env.standalone;

import space.arim.injector.Injector;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.omnibus.DefaultOmnibus;

import java.nio.file.Path;
import java.util.List;

public class UsageExample {

	// Prerequisites: snakeyaml and Gson dependencies on the classpath
	// Two items needed to implement:
	// 1. output for receiving messages (ConsoleAudience)
	// 2. input for dispatching commands (calls to CommandDispatch)
	public void compileMe() {
		// Define the data folder used for configuration, local databases, etc.
		Path dataFolder = Path.of("libertybans");
		// MUST IMPLEMENT: output for receiving messages
		// You may use the utility interface MessageOnlyAudience because Audience has unnecessary methods
		ConsoleAudience consoleAudience = consoleAudience();

		// Launch the punishment suite
		Injector injector = new StandaloneLauncher(
				dataFolder, new DefaultOmnibus()
		).createInjector(consoleAudience);
		// Retrieve the API
		LibertyBans api = injector.request(LibertyBans.class);
		// Retrieve the implementation
		BaseFoundation base = injector.request(BaseFoundation.class);
		// Startup
		base.startup();
		// MUST IMPLEMENT: input for dispatching commands
		CommandDispatch commandDispatch = injector.request(CommandDispatch.class);
		commandDispatch.accept("ban A248 Example ban");
		// You can use the API as usual
		// Careful - the following method loads ALL active punishments WITHOUT streaming or pagination
		// To use pagination I recommend the seekAfter methods (keyset pagination)
		@SuppressWarnings("unused")
		List<Punishment> punishments = api
				.getSelector()
				.selectionBuilder()
				.selectActiveOnly()
				//.seekAfter(/*...*/)
				.build()
				.getAllSpecificPunishments()
				.toCompletableFuture()
				.join(); // You may have better techniques depending on the usecase
		// Shutdown
		base.shutdown();
	}

	private ConsoleAudience consoleAudience() {
		throw new RuntimeException("Implement me");
	}

}
