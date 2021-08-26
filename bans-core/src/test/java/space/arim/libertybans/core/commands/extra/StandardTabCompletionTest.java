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

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.Time;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StandardTabCompletionTest {

	private final Configs configs;
	private final Provider<InternalDatabase> dbProvider;
	private final Time time;

	private TabCompletion tabCompletion;

	public StandardTabCompletionTest(@Mock Configs configs, @Mock Provider<InternalDatabase> dbProvider,
									 @Mock Time time) {
		this.configs = configs;
		this.dbProvider = dbProvider;
		this.time = time;
	}

	@BeforeEach
	public void setTabCompletion() {
		tabCompletion = new StandardTabCompletion(configs, dbProvider, time);
	}

	private void setUseOnlyPlayersOnSameServer(boolean useOnlyPlayersOnSameServer) {
		MainConfig mainConfig = mock(MainConfig.class);
		MainConfig.Commands commands = mock(MainConfig.Commands.class);
		TabCompletionConfig tabCompleteConfig = mock(TabCompletionConfig.class);
		when(configs.getMainConfig()).thenReturn(mainConfig);
		when(mainConfig.commands()).thenReturn(commands);
		when(commands.tabCompletion()).thenReturn(tabCompleteConfig);
		when(tabCompleteConfig.useOnlyPlayersOnSameServer()).thenReturn(useOnlyPlayersOnSameServer);
	}

	@Test
	public void completeOnlinePlayerNamesSameServer(@Mock CmdSender sender) {
		setUseOnlyPlayersOnSameServer(true);
		Set<String> allServers = Set.of("Sender", "Player1", "Player2");
		Set<String> sameServer = Set.of("Sender", "Player2");
		lenient().when(sender.getPlayerNames()).thenReturn(allServers.stream());
		lenient().when(sender.getPlayerNamesOnSameServer()).thenReturn(sameServer.stream());

		assertEquals(sameServer,
				tabCompletion.completeOnlinePlayerNames(sender).collect(Collectors.toUnmodifiableSet()));
	}

	@Test
	public void completeOnlinePlayerNamesAllServers(@Mock CmdSender sender) {
		setUseOnlyPlayersOnSameServer(false);
		Set<String> allServers = Set.of("Sender", "Player1", "Player2");
		Set<String> sameServer = Set.of("Sender", "Player2");
		lenient().when(sender.getPlayerNames()).thenReturn(allServers.stream());
		lenient().when(sender.getPlayerNamesOnSameServer()).thenReturn(sameServer.stream());

		assertEquals(allServers,
				tabCompletion.completeOnlinePlayerNames(sender).collect(Collectors.toUnmodifiableSet()));
	}
}
