/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
import jakarta.inject.Provider;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.alts.AddressManagement;
import space.arim.libertybans.core.alts.ConnectionLimitConfig;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static space.arim.libertybans.it.util.RandomUtil.randomAddress;
import static space.arim.libertybans.it.util.RandomUtil.randomName;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
public class ConnectionLimitIT {

	private final Provider<QueryExecutor> queryExecutor;
	private final SettableTime time;
	private final ConnectionLimitConfig conf;

	private AddressManagement limiter;

	@Inject
	public ConnectionLimitIT(Provider<QueryExecutor> queryExecutor, SettableTime time,
							 @Mock @DontInject ConnectionLimitConfig conf) {
		this.queryExecutor = queryExecutor;
		this.time = time;
		this.conf = conf;
	}

	@BeforeEach
	public void setupConfiguration() {
		when(conf.enable()).thenReturn(true);
		limiter = new AddressManagement(queryExecutor);
	}

	private Component exceededLimit(NetworkAddress address) {
		EnforcementConfig enforcementConfig = mock(EnforcementConfig.class);
		when(enforcementConfig.connectionLimiter()).thenReturn(conf);
		return queryExecutor.get().query((context) ->
				limiter.hasExceededLimit(context, enforcementConfig, address, time.currentTimestamp())
		).join();
	}

	@TestTemplate
	public void exceededLimit(Guardian guardian) {
		Component denialMessage = Component.text("Denied due to limit");
		when(conf.message()).thenReturn(denialMessage);
		when(conf.durationSeconds()).thenReturn(Duration.ofHours(4L).toSeconds());
		when(conf.limit()).thenReturn(2);

		NetworkAddress address = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(UUID.randomUUID(), randomName(), address).join());
		time.advanceBy(Duration.ofHours(1L));
		assumeTrue(null == guardian.executeAndCheckConnection(UUID.randomUUID(), randomName(), address).join());
		time.advanceBy(Duration.ofHours(1L));
		assumeTrue(null == guardian.executeAndCheckConnection(UUID.randomUUID(), randomName(), address).join());
		time.advanceBy(Duration.ofHours(1L));

		assertEquals(denialMessage, exceededLimit(address));
	}

	@TestTemplate
	public void sufficientTimePassed(Guardian guardian) {
		Component denialMessage = Component.text("Denied due to limit");
		when(conf.message()).thenReturn(denialMessage);
		when(conf.durationSeconds()).thenReturn(Duration.ofHours(2L).toSeconds());
		when(conf.limit()).thenReturn(2);

		NetworkAddress address = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(UUID.randomUUID(), randomName(), address).join());
		time.advanceBy(Duration.ofHours(1L));
		assumeTrue(null == guardian.executeAndCheckConnection(UUID.randomUUID(), randomName(), address).join());
		time.advanceBy(Duration.ofHours(1L));
		assumeTrue(null == guardian.executeAndCheckConnection(UUID.randomUUID(), randomName(), address).join());
		time.advanceBy(Duration.ofHours(1L));

		assertNull(exceededLimit(address));
	}
}
