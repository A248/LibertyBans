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

package space.arim.libertybans.it.test.command;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.commands.extra.StandardTabCompletion;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.commands.extra.TabCompletionConfig;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetTime;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static space.arim.libertybans.core.schema.tables.Names.NAMES;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
public class OfflineNamesIT {

	private final Configs configs;
	private final Provider<InternalDatabase> dbProvider;
	private final Time time;

	private TabCompletion tabCompletion;

	@Inject
	public OfflineNamesIT(@DontInject @Mock Configs configs, Provider<InternalDatabase> dbProvider, Time time) {
		this.configs = configs;
		this.dbProvider = dbProvider;
		this.time = time;
	}

	@BeforeEach
	public void setTabCompletion() {
		tabCompletion = new StandardTabCompletion(configs, dbProvider, time);
	}

	private static final long CURRENT_TIME = 1628954750;

	@TestTemplate
	@SetTime(unixTime = CURRENT_TIME)
	public void completeOfflinePlayerNames(
			@Mock @DontInject TabCompletionConfig.OfflinePlayerNames config,
			@Mock @DontInject CmdSender sender) {
		{
			MainConfig mainConfig = mock(MainConfig.class);
			MainConfig.Commands commands = mock(MainConfig.Commands.class);
			TabCompletionConfig tabCompleteConfig = mock(TabCompletionConfig.class);
			when(configs.getMainConfig()).thenReturn(mainConfig);
			when(mainConfig.commands()).thenReturn(commands);
			when(commands.tabCompletion()).thenReturn(tabCompleteConfig);
			when(tabCompleteConfig.offlinePlayerNames()).thenReturn(config);
		}
		when(config.enable()).thenReturn(true);
		when(config.retentionMinutes()).thenReturn(Duration.ofHours(3L).toMinutes());
		when(config.cacheRefreshSeconds()).thenReturn(Duration.ofMinutes(1L).toSeconds());

		Instant now = Instant.ofEpochSecond(CURRENT_TIME);
		dbProvider.get().execute((context) -> {
			context
					.insertInto(NAMES)
					.columns(NAMES.UUID, NAMES.NAME, NAMES.UPDATED)
					.values(UUID.randomUUID(), "Sender", now)
					.values(UUID.randomUUID(), "Player1", now)
					.values(UUID.randomUUID(), "Player2", now.minus(Duration.ofHours(1L)))
					.values(UUID.randomUUID(), "Player3", now.minus(Duration.ofHours(5L)))
					.execute();
		}).join();

		tabCompletion.startup();

		assertEquals(Set.of("Sender", "Player1", "Player2"),
				tabCompletion.completeOfflinePlayerNames(sender).collect(Collectors.toUnmodifiableSet()));
	}
}
