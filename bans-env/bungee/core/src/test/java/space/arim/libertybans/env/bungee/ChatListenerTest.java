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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatListenerTest {

	private final Guardian guardian;
	private final AddressReporter addressReporter;
	private final AudienceRepresenter<CommandSender> audienceRepresenter;

	private ChatListener chatListener;
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	public ChatListenerTest(@Mock Guardian guardian, @Mock AddressReporter addressReporter,
							@Mock AudienceRepresenter<CommandSender> audienceRepresenter) {
		this.guardian = guardian;
		this.addressReporter = addressReporter;
		this.audienceRepresenter = audienceRepresenter;
	}

	@BeforeEach
	public void setup() throws UnknownHostException {
		chatListener = new ChatListener(MockPlugin.create(), guardian, addressReporter, audienceRepresenter);
	}

	private void fire(ChatEvent event) {
		chatListener.onChatLow(event);
		chatListener.onChatHigh(event);
	}

	@Test
	public void allowNotMuted(@Mock ProxiedPlayer sender, @Mock Audience senderAudience,
							  @Mock ProxiedPlayer receiver) {
		String message = "hello";
		ChatEvent chatEvent = new ChatEvent(sender, receiver, message);
		when(guardian.checkChat(any(), (InetAddress) any(), any())).thenReturn(futuresFactory.completedFuture(null));
		lenient().when(audienceRepresenter.toAudience(sender)).thenReturn(senderAudience);

		fire(chatEvent);
		assertFalse(chatEvent.isCancelled());
		assertEquals(chatEvent.getMessage(), message, "Message untampered with");
		verifyNoMoreInteractions(senderAudience);
	}

	@Test
	public void enforceMuted(@Mock ProxiedPlayer sender, @Mock Audience senderAudience,
							 @Mock ProxiedPlayer receiver) {
		String message = "hello";
		ChatEvent chatEvent = new ChatEvent(sender, receiver, message);
		Component denyMessage = Component.text("Denied");
		when(guardian.checkChat(any(), (InetAddress) any(), isNull()))
				.thenReturn(futuresFactory.completedFuture(denyMessage));
		lenient().when(guardian.checkChat(any(), (InetAddress) any(), isNotNull()))
				.thenReturn(futuresFactory.completedFuture(null));
		when(audienceRepresenter.toAudience(sender)).thenReturn(senderAudience);

		fire(chatEvent);
		assertTrue(chatEvent.isCancelled());
		verify(senderAudience).sendMessage(denyMessage);
		verifyNoMoreInteractions(senderAudience);
	}
}
