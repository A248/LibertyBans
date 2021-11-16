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

package space.arim.libertybans.core.commands.usage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.env.CmdSender;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PluginInfoMessageTest {

	@Test
	public void fromReader() throws IOException {
		String text = String.join("\n", List.of(
				"Maintainer1",
				"Maintainer2",
				"",
				"Collaborator1",
				"Collaborator2",
				"",
				"Commended User1",
				"Commended User2"));
		assertEquals(new PluginInfoMessage(
				List.of("Maintainer1", "Maintainer2"),
				List.of("Collaborator1", "Collaborator2"),
				List.of("Commended User1", "Commended User2")),
				PluginInfoMessage.fromReader(new StringReader(text)));
	}

	@Test
	public void send(@Mock CmdSender sender) {
		new PluginInfoMessage(
				List.of("Maintainer"),
				List.of("Collaborator"),
				List.of("Commended User")
		).send(sender);
		verify(sender).sendLiteralMessage(notNull());
	}
}
