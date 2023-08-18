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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConnectionListenerTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private ConnectionListener listener;

	private final PluginContainer plugin;
	private final ProxyServer server;
	private final Guardian guardian;

	public ConnectionListenerTest(@Mock PluginContainer plugin, @Mock ProxyServer server,
								  @Mock Guardian guardian) {
		this.plugin = plugin;
		this.server = server;
		this.guardian = guardian;
	}

	@BeforeEach
	public void setup() {
		listener = new ConnectionListener(plugin, server, guardian);
	}

	private Player mockPlayer() {
		var mock = mock(Player.class);
		lenient().when(mock.getUniqueId()).thenReturn(UUID.randomUUID());
		lenient().when(mock.getUsername()).thenReturn("username");
		InetAddress address;
		try {
			address = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException ex) {
			throw new RuntimeException(ex);
		}
		lenient().when(mock.getRemoteAddress()).thenReturn(new InetSocketAddress(address, 0));
		return mock;
	}

	private void fixedResult(Component message) {
		var future = futuresFactory.completedFuture(message);
		when(guardian.executeAndCheckConnection(any(), any(), (InetAddress) any())).thenReturn(future);
		when(guardian.executeAndCheckConnection(any(), any(), (NetworkAddress) any())).thenReturn(future);
	}

	private void allowedResult() {
		fixedResult(null);
	}

	private void deniedResult(String text) {
		fixedResult(Component.text(text));
	}

	private void fireAndAwait(LoginEvent event) {
		EventTask task = listener.onConnect(event);
		if (task != null) {
			var future = new CompletableFuture<>();
			task.execute(new Continuation() {
				@Override
				public void resume() {
					future.complete(null);
				}

				@Override
				public void resumeWithException(Throwable exception) {
					future.completeExceptionally(exception);
				}
			});
			future.join();
		}
	}

	@Test
	public void allowed() {
		allowedResult();
		Player player = mockPlayer();
		LoginEvent event = new LoginEvent(player);
		var originalResult = event.getResult();
		fireAndAwait(event);
		assertEquals(originalResult.getReasonComponent(), event.getResult().getReasonComponent());
	}

	@Test
	public void denied() {
		deniedResult("denied");
		Player player = mockPlayer();
		LoginEvent event = new LoginEvent(player);
		fireAndAwait(event);
		assertEquals(
				"denied",
				event.getResult().getReasonComponent()
						.map(PlainComponentSerializer.plain()::serialize).orElse(null)
		);
	}

	@Test
	public void deniedByOtherPlugin() {
		allowedResult();
		Player player = mockPlayer();
		LoginEvent event = new LoginEvent(player);
		var denial = ResultedEvent.ComponentResult.denied(Component.text("denial"));
		event.setResult(denial);
		fireAndAwait(event);
		assertEquals(
				denial.getReasonComponent(),
				event.getResult().getReasonComponent()
		);
	}

}
