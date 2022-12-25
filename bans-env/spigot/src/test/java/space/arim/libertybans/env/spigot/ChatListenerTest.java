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

package space.arim.libertybans.env.spigot;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatListenerTest {

	private final Guardian guardian;
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	@TempDir
	public Path tempDir;

	private ChatListener chatListener;

	public ChatListenerTest(@Mock Guardian guardian) {
		this.guardian = guardian;
	}

	@BeforeEach
	public void setChatListener() {
		chatListener = new ChatListener(MockJavaPlugin.create(tempDir), guardian, Audience.class::cast);
	}

	private interface Player extends org.bukkit.entity.Player, Audience {

		@Override
		default void resetTitle() {}
	}

	@Test
	public void blockMuteCommand(@Mock Player player) throws UnknownHostException {
		UUID uuid = UUID.randomUUID();
		InetAddress address = InetAddress.getByName("127.0.0.1");
		Component denyMessage = Component.text("denied");

		when(player.getUniqueId()).thenReturn(uuid);
		when(player.getAddress()).thenReturn(new InetSocketAddress(address, 0));
		when(guardian.checkChat(uuid, address, "msg")).thenReturn(futuresFactory.completedFuture(denyMessage));

		PlayerCommandPreprocessEvent commandEvent = new PlayerCommandPreprocessEvent(player, "/msg", Set.of());
		chatListener.onCommand(commandEvent);

		assertTrue(commandEvent.isCancelled(), "Event should be cancelled");
		verify(player).sendMessage(denyMessage);
	}

}
