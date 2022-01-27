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

package space.arim.libertybans.it.test.punish.sync;

import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.core.punish.sync.SQLSynchronizationMessenger;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.util.RandomUtil;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@ExtendWith(InjectionInvocationContextProvider.class)
public class SQLSynchronizationIT {

	private final SQLSynchronizationMessenger synchronizationMessenger;
	private final SettableTime time;

	@Inject
	public SQLSynchronizationIT(SQLSynchronizationMessenger synchronizationMessenger, SettableTime time) {
		this.synchronizationMessenger = synchronizationMessenger;
		this.time = time;
	}

	@BeforeEach
	public void initialPoll() {
		synchronizationMessenger.setInitialTimestamp();
		time.advanceBy(Duration.ofMinutes(1L));
	}

	@AfterEach
	public void resetState() {
		synchronizationMessenger.resetLastTimestamp();
	}

	@TestTemplate
	public void noMessages() {
		assertArrayEquals(
				new byte[][] {},
				synchronizationMessenger.poll().join()
		);
	}

	private byte[] dispatchRandomMessage() {
		byte[] message = RandomUtil.randomBytes(ThreadLocalRandom.current().nextInt(5, 100));
		synchronizationMessenger.dispatch(message).join();
		return message;
	}

	@TestTemplate
	public void sendAndReceive() {
		byte[] message = dispatchRandomMessage();
		time.advanceBy(Duration.ofSeconds(10L));

		assertArrayEquals(
				new byte[][] {message},
				synchronizationMessenger.poll().join()
		);
	}

	@TestTemplate
	public void sendAndReceiveMultipleInOrder() {
		byte[] message1 = dispatchRandomMessage();
		time.advanceBy(Duration.ofSeconds(10L));
		byte[] message2 = dispatchRandomMessage();
		time.advanceBy(Duration.ofSeconds(300L));
		byte[] message3 = dispatchRandomMessage();
		time.advanceBy(Duration.ofSeconds(1L));
		byte[] message4 = dispatchRandomMessage();
		time.advanceBy(Duration.ofSeconds(5L));

		assertArrayEquals(
				new byte[][] {message1, message2, message3, message4},
				synchronizationMessenger.poll().join()
		);
	}
}
