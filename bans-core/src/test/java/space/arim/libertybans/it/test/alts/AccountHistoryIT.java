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

package space.arim.libertybans.it.test.alts;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.user.KnownAccount;
import space.arim.libertybans.core.alts.AccountHistory;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetTime;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.arim.libertybans.it.util.RandomUtil.randomAddress;

@ExtendWith(InjectionInvocationContextProvider.class)
public class AccountHistoryIT {

	private final AccountHistory accountHistory;

	private static final Duration ONE_DAY = Duration.ofDays(1L);

	@Inject
	public AccountHistoryIT(AccountHistory accountHistory) {
		this.accountHistory = accountHistory;
	}

	@TestTemplate
	public void listNoAccountHistory() {
		assertEquals(List.of(),
				accountHistory.knownAccounts(PlayerVictim.of(UUID.randomUUID())).join());
		assertEquals(List.of(),
				accountHistory.knownAccounts(AddressVictim.of(randomAddress())).join());
	}

	@TestTemplate
	@SetTime(unixTime = 1636233200)
	public void listAccountHistory(Guardian guardian, SettableTime time) {

		final Instant startTime = Instant.ofEpochSecond(1636233200);
		final Instant oneDayLater = startTime.plus(ONE_DAY);
		final Instant twoDaysLater = oneDayLater.plus(ONE_DAY);
		final Instant threeDaysLater = twoDaysLater.plus(ONE_DAY);

		UUID playerOne = UUID.randomUUID();
		String playerOneName = "Player1";
		UUID playerTwo = UUID.randomUUID();
		String playerTwoName = "Player2";
		NetworkAddress playerOneAddress = randomAddress();
		NetworkAddress playerTwoAddress = randomAddress();
		NetworkAddress sharedAddress = randomAddress();
		guardian.executeAndCheckConnection(playerOne, playerOneName, playerOneAddress).join();
		time.advanceBy(ONE_DAY);
		guardian.executeAndCheckConnection(playerTwo, playerTwoName, playerTwoAddress).join();
		time.advanceBy(ONE_DAY);
		guardian.executeAndCheckConnection(playerOne, playerOneName, sharedAddress).join();
		time.advanceBy(ONE_DAY);
		guardian.executeAndCheckConnection(playerTwo, playerTwoName, sharedAddress).join();

		assertEquals(
				List.of(accountHistory.newAccount(playerOne, playerOneName, playerOneAddress, startTime),
						accountHistory.newAccount(playerOne, playerOneName, sharedAddress, twoDaysLater)),
				accountHistory.knownAccounts(PlayerVictim.of(playerOne)).join());
		assertEquals(
				List.of(accountHistory.newAccount(playerTwo, playerTwoName, playerTwoAddress, oneDayLater),
						accountHistory.newAccount(playerTwo, playerTwoName, sharedAddress, threeDaysLater)),
				accountHistory.knownAccounts(PlayerVictim.of(playerTwo)).join());
		assertEquals(
				List.of(accountHistory.newAccount(playerOne, playerOneName, playerOneAddress, startTime)),
				accountHistory.knownAccounts(AddressVictim.of(playerOneAddress)).join());
		assertEquals(
				List.of(accountHistory.newAccount(playerTwo, playerTwoName, playerTwoAddress, oneDayLater)),
				accountHistory.knownAccounts(AddressVictim.of(playerTwoAddress)).join());
		assertEquals(
				List.of(accountHistory.newAccount(playerOne, playerOneName, sharedAddress, twoDaysLater),
						accountHistory.newAccount(playerTwo, playerTwoName, sharedAddress, threeDaysLater)),
				accountHistory.knownAccounts(AddressVictim.of(sharedAddress)).join());
	}

	@TestTemplate
	@SetTime(unixTime = 1636233200)
	public void deleteAccount(Guardian guardian, SettableTime time) {
		final Instant startTime = Instant.ofEpochSecond(1636233200);
		final Instant oneDayLater = startTime.plus(ONE_DAY);

		UUID player = UUID.randomUUID();
		String username = "Player1";
		NetworkAddress firstAddress = randomAddress();
		NetworkAddress secondAddress = randomAddress();

		guardian.executeAndCheckConnection(player, username, firstAddress).join();
		time.advanceBy(ONE_DAY);
		guardian.executeAndCheckConnection(player, username, secondAddress).join();

		assertFalse(
				accountHistory.deleteAccount(player, startTime.minus(ONE_DAY)).join(),
				"No account exists before startTime");

		KnownAccount firstAccount = accountHistory.newAccount(player, username, firstAddress, startTime);
		KnownAccount secondAccount = accountHistory.newAccount(player, username, secondAddress, oneDayLater);
		assertEquals(
				List.of(firstAccount, secondAccount),
				accountHistory.knownAccounts(PlayerVictim.of(player)).join());

		assertTrue(accountHistory.deleteAccount(player, startTime).join(), "Delete account at startTime");
		assertEquals(
				List.of(secondAccount),
				accountHistory.knownAccounts(PlayerVictim.of(player)).join());

		assertTrue(accountHistory.deleteAccount(player, oneDayLater).join(), "Delete account at oneDayLater");
		assertEquals(
				List.of(),
				accountHistory.knownAccounts(PlayerVictim.of(player)).join());
	}

	@TestTemplate
	@SetTime(unixTime = 1636233200)
	public void deleteAccountThroughApi(Guardian guardian, SettableTime time) {
		final Instant startTime = Instant.ofEpochSecond(1636233200);

		UUID player = UUID.randomUUID();
		String username = "Player1";
		NetworkAddress firstAddress = randomAddress();
		NetworkAddress secondAddress = randomAddress();

		guardian.executeAndCheckConnection(player, username, firstAddress).join();
		time.advanceBy(ONE_DAY);
		guardian.executeAndCheckConnection(player, username, secondAddress).join();

		KnownAccount firstAccount = accountHistory.newAccount(player, username, firstAddress, startTime);
		KnownAccount secondAccount = accountHistory.newAccount(player, username, secondAddress, startTime.plus(ONE_DAY));
		assertEquals(
				List.of(firstAccount, secondAccount),
				accountHistory.knownAccounts(PlayerVictim.of(player)).join());

		assertTrue(firstAccount.deleteFromHistory().join(), "Delete first account");
		assertEquals(
				List.of(secondAccount),
				accountHistory.knownAccounts(PlayerVictim.of(player)).join());

		assertTrue(secondAccount.deleteFromHistory().join(), "Delete second account");
		assertEquals(
				List.of(),
				accountHistory.knownAccounts(PlayerVictim.of(player)).join());
	}

}
