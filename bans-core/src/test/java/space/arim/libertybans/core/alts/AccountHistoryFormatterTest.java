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

package space.arim.libertybans.core.alts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.user.KnownAccount;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.database.execute.QueryExecutor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountHistoryFormatterTest {

	private final AccountHistorySection conf;
	private final InternalFormatter formatter;

	private AccountHistoryFormatter accountHistoryFormatter;

	public AccountHistoryFormatterTest(@Mock AccountHistorySection conf, @Mock InternalFormatter formatter) {
		this.conf = conf;
		this.formatter = formatter;
	}

	@BeforeEach
	public void setAccountHistoryFormatter(@Mock Configs configs, @Mock MessagesConfig messagesConfig) {
		when(configs.getMessagesConfig()).thenReturn(messagesConfig);
		when(messagesConfig.accountHistory()).thenReturn(conf);
		accountHistoryFormatter = new AccountHistoryFormatter(configs, formatter);
	}

	@Test
	public void formatMessage() throws UnknownHostException {
		ComponentText header = ComponentText.create(Component.text("Known accounts for %TARGET%"));
		UUID userId = UUID.randomUUID();
		String username = "TargetUser";
		String address = "207.144.101.102";
		Instant date = Instant.parse("2021-07-23T02:15:23.000000Z");

		AccountHistory accountHistory = new AccountHistory(() -> mock(QueryExecutor.class));
		KnownAccount knownAccount = accountHistory.newAccount(
				userId, username, NetworkAddress.of(InetAddress.getByName(address)), date);

		{
			AccountHistorySection.Listing listing = mock(AccountHistorySection.Listing.class);
			when(conf.listing()).thenReturn(listing);
			when(listing.header()).thenReturn(header);
			when(listing.layout()).thenReturn(ComponentText.create(Component.text(
					"username: %USERNAME%, address: %ADDRESS%, date_recorded: %DATE_RECORDED%")));
			when(formatter.prefix(any())).thenAnswer((invocation) -> invocation.getArgument(0));
			when(formatter.formatAbsoluteDate(date)).thenReturn(date.toString());
		}
		assertEquals("Known accounts for TargetUser\n" +
						"username: " + username + ", address: " + address + ", date_recorded: " + date,
				PlainComponentSerializer.plain().serialize(accountHistoryFormatter.formatMessage(username, List.of(knownAccount))));
	}
}
