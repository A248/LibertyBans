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

package space.arim.libertybans.core.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(CommandSetupExtension.class)
public class ListCommandsTest {

	private ListCommands listCommands;
	private final PunishmentSelector selector;
	private final InternalFormatter formatter;

	public ListCommandsTest(@Mock PunishmentSelector selector, @Mock InternalFormatter formatter) {
		this.selector = selector;
		this.formatter = formatter;
	}

	@BeforeEach
	public void setListCommands(AbstractSubCommandGroup.Dependencies dependencies) {
		listCommands = new ListCommands(dependencies, selector, formatter);
	}

	@Test
	public void suggest(@Mock CmdSender sender) {
		Set<String> playerNames = Set.of("player1", "player2");
		when(sender.getPlayersOnSameServer()).thenReturn(playerNames.stream());
		assertEquals(playerNames, listCommands.suggest(sender, "history", 0).collect(Collectors.toUnmodifiableSet()));
	}
}
