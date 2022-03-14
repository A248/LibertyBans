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

package space.arim.libertybans.core.selector.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.core.service.SettableTimeImpl;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
public class OnDemandMuteCacheTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final InternalSelector selector;
	private final SettableTime time = new SettableTimeImpl(Instant.EPOCH);

	private UUID uuid;
	private NetworkAddress address;
	private MuteCache muteCache;

	private static final Duration EXPIRATION_TIME = Duration.ofSeconds(20L);

	public OnDemandMuteCacheTest(@Mock InternalSelector selector) {
		this.selector = selector;
	}

	@BeforeEach
	public void setMuteCache(@Mock InternalFormatter formatter, @Mock Configs configs, @Mock SqlConfig sqlConfig,
							 @Mock SqlConfig.MuteCaching muteCaching, @Mock SqlConfig.Synchronization synchronization) {
		when(configs.getSqlConfig()).thenReturn(sqlConfig);
		when(sqlConfig.muteCaching()).thenReturn(muteCaching);
		when(muteCaching.expirationTimeSeconds()).thenReturn((int) EXPIRATION_TIME.toSeconds());
		when(muteCaching.expirationSemantic()).thenReturn(SqlConfig.MuteCaching.ExpirationSemantic.EXPIRE_AFTER_WRITE);
		when(sqlConfig.synchronization()).thenReturn(synchronization);
		when(synchronization.enabled()).thenReturn(false);

		muteCache = new OnDemandMuteCache(configs, futuresFactory, selector, formatter, time);
		muteCache.startup();

		uuid = UUID.randomUUID();
		address = RandomUtil.randomAddress();
	}

	@Test
	public void notMuted() {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.empty()));

		assertEquals(Optional.empty(), muteCache.getCachedMute(uuid, address).join());
	}

	@Test
	public void efficiencyOfReuse(@Mock Punishment punishment) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(punishment)));
		when(punishment.isExpired(any())).thenReturn(false);

		assertEquals(Optional.of(punishment), muteCache.getCachedMute(uuid, address).join());

		// Wait a bit before the next call, but not beyond the expiration time
		time.advanceBy(Duration.ofSeconds(3L));
		assertEquals(Optional.of(punishment), muteCache.getCachedMute(uuid, address).join());

		// Make sure the database was only queried once
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);
	}

	@Test
	public void recomputeAfterCacheExpiry(@Mock Punishment punishment) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(punishment)));
		when(punishment.isExpired(any())).thenReturn(false);

		assertEquals(Optional.of(punishment), muteCache.getCachedMute(uuid, address).join());

		// Wait beyond the expiration time
		time.advanceBy(EXPIRATION_TIME.plusSeconds(1L));
		assertEquals(Optional.of(punishment), muteCache.getCachedMute(uuid, address).join());

		// Make sure the database was indeed queried twice
		verify(selector, times(2)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);
	}

	@Test
	public void cachedPunishmentItselfExpires(@Mock Punishment punishment) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(punishment)));
		when(punishment.isExpired(any())).thenReturn(false);

		assertEquals(Optional.of(punishment), muteCache.getCachedMute(uuid, address).join());

		// Expire the punishment
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.empty()));
		when(punishment.isExpired(any())).thenReturn(true);

		assertEquals(Optional.empty(), muteCache.getCachedMute(uuid, address).join());
	}

	@Test
	public void doNotReplaceOlderMuteDueToItsLaterEndDate(@Mock Punishment oldMute, @Mock Punishment newMute) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(oldMute)));
		when(oldMute.isExpired(any())).thenReturn(false);
		when(newMute.getType()).thenReturn(PunishmentType.MUTE);
		when(oldMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(200L)));
		when(newMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(100L)));

		assertEquals(Optional.of(oldMute), muteCache.getCachedMute(uuid, address).join());

		muteCache.setCachedMute(uuid, address, newMute);

		assertEquals(Optional.of(oldMute), muteCache.getCachedMute(uuid, address).join());
	}

	@Test
	public void replaceOlderMuteDueToItsSoonerEndDate(@Mock Punishment oldMute, @Mock Punishment newMute) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(oldMute)));
		when(oldMute.isExpired(any())).thenReturn(false);
		when(newMute.isExpired(any())).thenReturn(false);
		when(newMute.getType()).thenReturn(PunishmentType.MUTE);
		when(oldMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(100L)));
		when(newMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(200L)));

		assertEquals(Optional.of(oldMute), muteCache.getCachedMute(uuid, address).join());

		muteCache.setCachedMute(uuid, address, newMute);

		assertEquals(Optional.of(newMute), muteCache.getCachedMute(uuid, address).join());
	}
}
