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

package space.arim.libertybans.env.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatListenerTest {

	private final Enforcer enforcer;
	private final AddressReporter addressReporter;
	private final PlatformHandle handle;

	private ChatListener chatListener;
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	public ChatListenerTest(@Mock Enforcer enforcer, @Mock AddressReporter addressReporter,
							@Mock PlatformHandle handle) {
		this.enforcer = enforcer;
		this.addressReporter = addressReporter;
		this.handle = handle;
	}

	@BeforeEach
	public void setup() {
		chatListener = new ChatListener(MockPlugin.create(), enforcer, addressReporter, handle);
	}

	private void fire(ChatEvent event) {
		chatListener.onChatLow(event);
		chatListener.onChatHigh(event);
	}

	@Test
	public void allowNotMuted(@Mock ProxiedPlayer sender, @Mock ProxiedPlayer receiver) {
		String message = "hello";
		ChatEvent chatEvent = new ChatEvent(sender, receiver, message);
		when(enforcer.checkChat(any(), (InetAddress) any(), any())).thenReturn(futuresFactory.completedFuture(null));

		fire(chatEvent);
		assertFalse(chatEvent.isCancelled());
		assertEquals(chatEvent.getMessage(), message, "Message untampered with");
		verifyNoMoreInteractions(handle);
	}

	@Test
	public void enforceMuted(@Mock ProxiedPlayer sender, @Mock ProxiedPlayer receiver) {
		String message = "hello";
		ChatEvent chatEvent = new ChatEvent(sender, receiver, message);
		SendableMessage denyMessage = SendableMessage.empty();
		when(enforcer.checkChat(any(), (InetAddress) any(), any()))
				.thenReturn(futuresFactory.completedFuture(denyMessage));

		fire(chatEvent);
		assertTrue(chatEvent.isCancelled());
		verify(handle).sendMessage(sender, denyMessage);
	}
}
