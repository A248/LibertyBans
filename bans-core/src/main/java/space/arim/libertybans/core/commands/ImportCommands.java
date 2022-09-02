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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.importing.AdvancedBanImportSource;
import space.arim.libertybans.core.importing.BanManagerImportSource;
import space.arim.libertybans.core.importing.ImportExecutor;
import space.arim.libertybans.core.importing.ImportSource;
import space.arim.libertybans.core.importing.ImportStatistics;
import space.arim.libertybans.core.importing.LiteBansImportSource;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.importing.SelfImportProcess;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Singleton
public final class ImportCommands extends AbstractSubCommandGroup {

	private final ImportExecutor executor;
	private final Map<PluginSourceType, Provider<? extends ImportSource>> importSourceProviders;
	/** The self-import process has no ImportSource implementation */
	private final SelfImportProcess selfImportProcess;
	private final AtomicBoolean isImporting = new AtomicBoolean();

	@Inject
	public ImportCommands(Dependencies dependencies, ImportExecutor executor,
						  Provider<AdvancedBanImportSource> advancedBanImportSourceProvider,
						  Provider<BanManagerImportSource> banManagerImportSourceProvider,
						  Provider<LiteBansImportSource> liteBansImportSourceProvider,
						  Provider<PlatformImportSource> platformImportSourceProvider,
						  SelfImportProcess selfImportProcess) {
		super(dependencies, "import");
		this.executor = executor;
		importSourceProviders = Map.of(
				PluginSourceType.ADVANCEDBAN, advancedBanImportSourceProvider,
				PluginSourceType.BANMANAGER, banManagerImportSourceProvider,
				PluginSourceType.LITEBANS, liteBansImportSourceProvider,
				PluginSourceType.VANILLA, platformImportSourceProvider);
		this.selfImportProcess = selfImportProcess;
	}

	private enum PluginSourceType {
		ADVANCEDBAN,
		BANMANAGER,
		LITEBANS,
		VANILLA,
		SELF
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			return Stream.of(PluginSourceType.values())
					.map((pluginSourceType) -> pluginSourceType.name().toLowerCase(Locale.ROOT));
		}
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.admin.import");
	}

	private class Execution extends AbstractCommandExecution {

		Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		@Override
		public ReactionStage<Void> execute() {
			if (!hasPermission(sender())) {
				sender().sendMessage(messages().admin().noPermission());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(importMessages().usage());
				return null;
			}
			String pluginSource = command().next().toUpperCase(Locale.ROOT);
			PluginSourceType sourceType;
			try {
				sourceType = PluginSourceType.valueOf(pluginSource);
			} catch (IllegalArgumentException ex) {
				sender().sendMessage(importMessages().usage());
				return null;
			}
			if (!isImporting.compareAndSet(false, true)) {
				sender().sendMessage(importMessages().inProgress());
				return null;
			}
			CentralisedFuture<Void> importFuture;
			if (sourceType == PluginSourceType.SELF) {
				importFuture = selfImportProcess.transferAllData();
			} else {
				ImportSource importSource = importSourceProviders.get(sourceType).get();
				importFuture = executor.performImport(importSource).thenAccept((ImportStatistics statistics) -> {
					if (statistics.success()) {
						sender().sendMessage(importMessages().complete());
						sender().sendLiteralMessage(statistics.toString());
					} else {
						sender().sendMessage(importMessages().failure());
					}
				});
			}
			sender().sendMessage(importMessages().started());
			return importFuture.whenComplete((ignore, ex) -> isImporting.set(false));
		}

		private MessagesConfig.Admin.Importing importMessages() {
			return messages().admin().importing();
		}

	}

}
