/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.env;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.omnibus.util.ThisClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Singleton
public final class EnvironmentManager implements Part {

	private final Environment environment;
	private final Configs configs;
	private final EnvServerNameDetection serverNameDetection;
	private final InternalScopeManager scopeManager;

	private Set<PlatformListener> listeners;
	private AliasCommand[] readyAliases;

	private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public EnvironmentManager(Environment environment, Configs configs,
							  EnvServerNameDetection serverNameDetection, InternalScopeManager scopeManager) {
		this.environment = environment;
		this.configs = configs;
		this.serverNameDetection = serverNameDetection;
		this.scopeManager = scopeManager;
	}

	public Object platformAccess() {
		return environment.platformAccess();
	}

	private void registerListeners() {
		Set<PlatformListener> listeners = environment.createListeners(new AliasCommand.RegisterOutcome() {
			@Override
			public void success() {}

			@Override
			public void deregistrationUnavailable() {
				disappear();
			}

			@Override
			public void alreadyRegistered(Object existingCommand, @Nullable String belongingTo) {
				LOGGER.error(
						"Failed to register root command '/libertybans'. It already exists as {}{}",
						existingCommand, belongingTo == null ? "" : " belonging to " + belongingTo
				);
			}

			@Override
			public void disappear() {
				LOGGER.error("Failed to register root command '/libertybans'.");
			}
		});
		listeners.forEach(PlatformListener::register);
		this.listeners = listeners;
	}

	private void registerAliases() {
		enum FailureReason {
			SELF_DUP,
			UNSUPPORTED,
			NO_DEREG,
			CONFLICT,
			DISAPPEARED
			;

			String printTitle() {
				return switch (this) {
					case SELF_DUP -> "Duplicates";
					case UNSUPPORTED -> "Unsupported aliases";
					case NO_DEREG -> "Deregistration unsupported";
					case CONFLICT -> "Conflicts";
					case DISAPPEARED -> "Disappeared";
				};
			}

			String printIntro() {
				return switch (this) {
					case SELF_DUP -> "The following commands were duplicated in the configuration. Please remove the duplicates (they will be skipped for now).";
					case UNSUPPORTED -> """
                            The server platform does not support registering these commands. Some platforms do not let \
                            us register commands dynamically (e.g. Sponge), and on others like Fabric, we don't have an easy way to replace vanilla commands.
                            
                            Sometimes, you can use a full alias plugin to accomplish what LibertyBans cannot. Search for alias plugins online. Here are the aliases we could not register:""";
					case NO_DEREG -> "The server platform only partially supports registering custom aliases. It does " +
							"not allow us to deregister aliases, which would cause very confusing behavior. The following commands therefore have been disabled.";
					case CONFLICT -> """
                            LibertyBans attempted to register these commands, but they already already exist and may belong to other plugins.
                            For each of the commands, consider if you want LibertyBans to control this command. If yes, you must solve the command registration
                            conflict with the other plugin:
                            1. First check if the other plugin has an option to disable the command. If it does, use it.
                               Good plugins will provide this option, but many, including Essentials, do not.
                            2. On Bukkit/Spigot/Paper, you can use the server's commands.yml to specify command overrides.
                               You can find information about this at https://bukkit.fandom.com/wiki/Commands.yml
                            3. It is also possible to use an alias plugin to specify which plugin uses the command.
                               Many alias plugins exist on popular plugin release websites.
                            
                            If you do not want LibertyBans to control a command, you should disable it in the
                            alias configuration.
                            
                            Here are the commands that could not be registered:""";
					case DISAPPEARED -> "These commands did not appear in the server's APIs after registration. This " +
							"suggests the server platform is bugged or wrongly implemented.";
				};
			}
		}
		record Failure(FailureReason reason, String alias, String base,
					   @Nullable Object existingCommand, @Nullable String belongingTo) {
			void printTo(StringBuilder errorReport) {
				if (reason != FailureReason.SELF_DUP && alias.equals(base)) {
					errorReport.append(alias);
				} else {
					errorReport.append(alias).append(" -> ").append(base);
				}
				if (reason == FailureReason.CONFLICT) {
					errorReport.append(" (existing command ").append(existingCommand);
					if (belongingTo != null) {
						errorReport.append(" belonging to ");
						errorReport.append(belongingTo);
					}
					errorReport.append(')');
				}
			}
		}
		List<String> mappedAliases = configs.getMainConfig().commands().aliases();
		Map<String, @Nullable AliasCommand> aliasCommands = new LinkedHashMap<>();
		class Outcomes {
			int successes;
			final List<Failure> failures = new ArrayList<>();
		}
		Outcomes outcomes = new Outcomes();

		for (String mapping : mappedAliases) {
			String alias, base;
			if (mapping.indexOf(':') == -1) {
				alias = base = mapping;
			} else {
				String[] split = mapping.split(":");
				alias = split[0];
				base = split[1];
			}
			if (aliasCommands.containsKey(alias)) {
				outcomes.failures.add(new Failure(
						FailureReason.SELF_DUP, alias, base, null, null
				));
				continue;
			}
			AliasCommand aliasCommand = environment.createAliasCommand(alias, base);
			aliasCommands.put(alias, aliasCommand);
			if (aliasCommand == null) {
				outcomes.failures.add(new Failure(
						FailureReason.UNSUPPORTED, alias, base, null, null
				));
			} else {
				aliasCommand.register(new AliasCommand.RegisterOutcome() {
					@Override
					public void success() {
						outcomes.successes++;
					}

					@Override
					public void deregistrationUnavailable() {
						outcomes.failures.add(new Failure(FailureReason.NO_DEREG, alias, base, null, null));
					}

					@Override
					public void alreadyRegistered(Object existingCommand, @Nullable String belongingTo) {
						outcomes.failures.add(new Failure(
								FailureReason.CONFLICT, alias, base, existingCommand, belongingTo
						));
					}

					@Override
					public void disappear() {}
				});
			}
		}
		if (outcomes.successes != 0) {
			LOGGER.info("Registered {} alias commands successfully.", outcomes.successes);
		}
		if (!outcomes.failures.isEmpty()) {
			StringBuilder errorReport = new StringBuilder();
			errorReport.append("Encountered one or more problems while attempting to register commands. Please read below and take appropriate action.");
			Map<FailureReason, List<Failure>> sortedFailures = new HashMap<>();
			for (FailureReason reason : FailureReason.values()) {
				sortedFailures.put(reason, new ArrayList<>());
			}
			for (Failure failure : outcomes.failures) {
				sortedFailures.get(failure.reason).add(failure);
			}
			for (FailureReason reason : FailureReason.values()) {
				List<Failure> failuresHere = sortedFailures.get(reason);
				if (failuresHere.isEmpty()) {
					continue;
				}
				errorReport.append("\n\n====================================================================================================\n");
				errorReport.append(reason.printTitle().toUpperCase(Locale.ROOT));
				errorReport.append("\n\n");
				errorReport.append(reason.printIntro());
				int count = 0;
				for (Failure failureHere : failuresHere) {
					errorReport.append('\n');
					errorReport.append(++count);
					errorReport.append(".      ");
					failureHere.printTo(errorReport);
				}
			}
			errorReport.append('\n');
		}
		readyAliases = aliasCommands.values().stream().filter(Objects::nonNull).toArray(AliasCommand[]::new);
	}

	@Override
	public void startup() {
		registerListeners();
		registerAliases();
		serverNameDetection.detectName(scopeManager);
	}

	@Override
	public void restart() {
		throw new StartupException("Internal error, EnvironmentManager#restart should not be called");
	}

	@Override
	public void shutdown() {
		listeners.forEach(PlatformListener::unregister);
		listeners = null;
		for (AliasCommand commandAlias : readyAliases) {
			commandAlias.unregister();
		}
		scopeManager.clearDetectedServerName();
	}

}
