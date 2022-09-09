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

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.core.service.SettableTimeImpl;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static space.arim.libertybans.core.selector.cache.AlwaysAvailableMuteCache.GRACE_PERIOD_NANOS;
import static space.arim.libertybans.core.selector.cache.AlwaysAvailableMuteCache.PURGE_TASK_INTERVAL;

@ExtendWith(MockitoExtension.class)
public class AlwaysAvailableMuteCacheTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final InternalSelector selector;
	private final EnhancedExecutor enhancedExecutor;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private final EnvUserResolver envUserResolver;
	private final InternalFormatter formatter;
	private final SettableTime time = new SettableTimeImpl(Instant.EPOCH);

	private UUID uuid;
	private NetworkAddress address;
	private MuteCache muteCache;

	// Make sure expiration time > purge task interval
	private static final Duration EXPIRATION_TIME = PURGE_TASK_INTERVAL
			.multipliedBy(3L)
			.plus(Duration.ofSeconds(20L));

	public AlwaysAvailableMuteCacheTest(@Mock InternalSelector selector, @Mock EnhancedExecutor enhancedExecutor,
										@Mock EnvUserResolver envUserResolver, @Mock InternalFormatter formatter) {
		this.selector = selector;
		this.enhancedExecutor = enhancedExecutor;
		this.envUserResolver = envUserResolver;
		this.formatter = formatter;
	}

	@BeforeEach
	public void setMuteCache(@Mock Configs configs, @Mock SqlConfig sqlConfig,
							 @Mock SqlConfig.MuteCaching muteCaching, @Mock SqlConfig.Synchronization synchronization) {
		when(configs.getSqlConfig()).thenReturn(sqlConfig);
		when(sqlConfig.muteCaching()).thenReturn(muteCaching);
		when(muteCaching.expirationTimeSeconds()).thenReturn((int) EXPIRATION_TIME.toSeconds());
		when(muteCaching.expirationSemantic()).thenReturn(SqlConfig.MuteCaching.ExpirationSemantic.EXPIRE_AFTER_WRITE);
		when(sqlConfig.synchronization()).thenReturn(synchronization);
		when(synchronization.enabled()).thenReturn(false);
		when(enhancedExecutor.scheduleRepeating((Runnable) any(), any(), any())).thenAnswer((invocation) -> {
			Runnable command = invocation.getArgument(0);
			scheduledTasks.add(command);
			return null;
		});

		muteCache = new AlwaysAvailableMuteCache(
				configs, futuresFactory, selector, enhancedExecutor, envUserResolver, formatter, time);
		muteCache.startup();

		uuid = UUID.randomUUID();
		address = RandomUtil.randomAddress();
		when(envUserResolver.lookupName(uuid)).thenReturn(futuresFactory.completedFuture(Optional.of("Username")));
	}

	private void assertAvailableCacheResult(@Nullable Component muteMessage) {
		CentralisedFuture<Optional<Component>> futureMessage = muteCache.getCachedMuteMessage(uuid, address);
		assertTrue(futureMessage.isDone());
		assertEquals(Optional.ofNullable(muteMessage), futureMessage.join());
	}

	private void runScheduledTasks() {
		scheduledTasks.forEach(Runnable::run);
	}

	// cacheOnLogin

	@Test
	public void cacheOnLogin() {
		CentralisedFuture<Optional<Punishment>> databaseQuery = futuresFactory.newIncompleteFuture();
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE)).thenReturn(databaseQuery);
		CentralisedFuture<?> cacheOnLogin = muteCache.cacheOnLogin(uuid, address);
		assertFalse(cacheOnLogin.isDone());
		// Purge task should be OK since only 5 seconds have passed
		time.advanceBy(Duration.ofSeconds(5L));
		runScheduledTasks();
		// Complete the query, which allows the login to complete
		databaseQuery.complete(Optional.empty());
		assertTrue(cacheOnLogin.isDone());
	}

	@Test
	public void muteIsNotAvailableBeforeLogin() {
		CentralisedFuture<Optional<Punishment>> databaseQuery = futuresFactory.newIncompleteFuture();
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE)).thenReturn(databaseQuery);
		CentralisedFuture<Optional<Punishment>> cachedMute = muteCache.getCachedMute(uuid, address);
		assertFalse(cachedMute.isDone());
		// Complete the query, which allows the request to complete
		databaseQuery.complete(Optional.empty());
		assertTrue(cachedMute.isDone());
		assertEquals(Optional.empty(), cachedMute.join());
	}

	@Test
	public void muteIsAlwaysAvailableAfterLogin(@Mock Punishment mute) {
		Component muteMessage = Component.text("You are muted");

		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(mute)));
		when(formatter.getPunishmentMessage(mute)).thenReturn(futuresFactory.completedFuture(muteMessage));
		muteCache.cacheOnLogin(uuid, address).join();
		runScheduledTasks();

		// Check availability
		assertAvailableCacheResult(muteMessage);

		// Wait some time, then try again
		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(muteMessage);

		// Wait again, this time running the purge task
		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		assertAvailableCacheResult(muteMessage);

		// Wait past expiration time
		time.advanceBy(EXPIRATION_TIME);
		assertAvailableCacheResult(muteMessage);

		// Try one more time
		time.advanceBy(Duration.ofSeconds(2L));
		assertAvailableCacheResult(muteMessage);
	}

	@Test
	public void lackOfMuteIsAlwaysAvailableAfterLogin() {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.empty()));
		muteCache.cacheOnLogin(uuid, address).join();

		// Check availability
		assertAvailableCacheResult(null);

		// Wait some time, then try again
		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(null);

		// Wait again, this time running the purge task
		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		assertAvailableCacheResult(null);

		// Wait past expiration time
		time.advanceBy(EXPIRATION_TIME);
		assertAvailableCacheResult(null);

		// Try one more time
		time.advanceBy(Duration.ofSeconds(2L));
		assertAvailableCacheResult(null);
	}

	// cacheRequest

	@Test
	public void updateToMuteAfterExpirationTime(@Mock Punishment mute) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.empty()));
		muteCache.cacheOnLogin(uuid, address).join();

		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(null);

		// Set the new mute
		Component muteMessage = Component.text("Muted forever");
		CentralisedFuture<Optional<Punishment>> futurePunishment = futuresFactory.newIncompleteFuture();
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE)).thenReturn(futurePunishment);
		when(formatter.getPunishmentMessage(mute)).thenReturn(futuresFactory.completedFuture(muteMessage));

		// Wait, but not past expiration time. The old mute should be used
		time.advanceBy(Duration.ofSeconds(3L));
		assertAvailableCacheResult(null);
		// Make sure the new computation hasn't yet been triggered
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// Wait again, this time running the purge task
		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		assertAvailableCacheResult(null);
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// Wait past the expiration time, triggering the new computation
		time.advanceBy(EXPIRATION_TIME);
		assertAvailableCacheResult(null);
		// The new computation should have been triggered
		verify(selector, times(2)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// The new computation has been initiated, but is not yet available
		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(null);

		// Finish the new computation and assert its new presence
		futurePunishment.complete(Optional.of(mute));
		assertAvailableCacheResult(muteMessage);
	}

	@Test
	public void clearMuteAfterExpirationTime(@Mock Punishment mute) {
		Component muteMessage = Component.text("Muted forever");
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(mute)));
		when(formatter.getPunishmentMessage(mute)).thenReturn(futuresFactory.completedFuture(muteMessage));

		muteCache.cacheOnLogin(uuid, address).join();

		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(muteMessage);

		// Clear the mute
		CentralisedFuture<Optional<Punishment>> futurePunishment = futuresFactory.newIncompleteFuture();
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE)).thenReturn(futurePunishment);

		// Wait, but not past expiration time. The old mute should be used
		time.advanceBy(Duration.ofSeconds(3L));
		assertAvailableCacheResult(muteMessage);
		// Make sure the new computation hasn't yet been triggered
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// Wait again, this time running the purge task
		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		assertAvailableCacheResult(muteMessage);
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// Wait past the expiration time, triggering the new computation
		time.advanceBy(EXPIRATION_TIME);
		assertAvailableCacheResult(muteMessage);
		// The new computation should have been triggered
		verify(selector, times(2)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// The new computation has been initiated, but is not yet available
		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(muteMessage);

		// Finish the new computation and assert its new presence
		futurePunishment.complete(Optional.empty());
		assertAvailableCacheResult(null);
	}

	@Test
	public void updateToNewerMute(@Mock Punishment oldMute, @Mock Punishment newMute) {
		Component oldMuteMessage = Component.text("Muted for some reason");
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(oldMute)));
		when(formatter.getPunishmentMessage(oldMute)).thenReturn(futuresFactory.completedFuture(oldMuteMessage));

		muteCache.cacheOnLogin(uuid, address).join();

		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(oldMuteMessage);

		// Set the new mute
		Component newMuteMessage = Component.text("Muted forever");
		CentralisedFuture<Optional<Punishment>> futurePunishment = futuresFactory.newIncompleteFuture();
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE)).thenReturn(futurePunishment);
		when(formatter.getPunishmentMessage(newMute)).thenReturn(futuresFactory.completedFuture(newMuteMessage));

		// Wait, but not past expiration time. The old mute should be used
		time.advanceBy(Duration.ofSeconds(3L));
		assertAvailableCacheResult(oldMuteMessage);
		// Make sure the new computation hasn't yet been triggered
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// Wait again, this time running the purge task
		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		assertAvailableCacheResult(oldMuteMessage);
		verify(selector, times(1)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// Wait past the expiration time, triggering the new computation
		time.advanceBy(EXPIRATION_TIME);
		assertAvailableCacheResult(oldMuteMessage);
		// The new computation should have been triggered
		verify(selector, times(2)).getApplicablePunishment(uuid, address, PunishmentType.MUTE);

		// The new computation has been initiated, but is not yet available
		time.advanceBy(Duration.ofSeconds(1L));
		assertAvailableCacheResult(oldMuteMessage);

		// Finish the new computation and assert its new presence
		futurePunishment.complete(Optional.of(newMute));
		assertAvailableCacheResult(newMuteMessage);
	}

	// setCachedMute

	@Test
	public void doNotReplaceOlderMuteDueToItsLaterEndDate(@Mock Punishment oldMute, @Mock Punishment newMute) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(oldMute)));
		when(formatter.getPunishmentMessage(oldMute)).thenReturn(futuresFactory.completedFuture(Component.empty()));
		when(formatter.getPunishmentMessage(newMute)).thenReturn(futuresFactory.completedFuture(Component.empty()));

		when(newMute.getType()).thenReturn(PunishmentType.MUTE);
		when(oldMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(200L)));
		when(newMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(100L)));

		muteCache.cacheOnLogin(uuid, address).join();
		assertEquals(Optional.of(oldMute), muteCache.getCachedMute(uuid, address).join());

		muteCache.setCachedMute(uuid, address, newMute);
		assertEquals(Optional.of(oldMute), muteCache.getCachedMute(uuid, address).join());
	}

	@Test
	public void replaceOlderMuteDueToItsSoonerEndDate(@Mock Punishment oldMute, @Mock Punishment newMute) {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.of(oldMute)));
		when(formatter.getPunishmentMessage(oldMute)).thenReturn(futuresFactory.completedFuture(Component.empty()));
		when(formatter.getPunishmentMessage(newMute)).thenReturn(futuresFactory.completedFuture(Component.empty()));

		when(newMute.getType()).thenReturn(PunishmentType.MUTE);
		when(oldMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(100L)));
		when(newMute.getEndDate()).thenReturn(Instant.EPOCH.plus(Duration.ofDays(200L)));

		muteCache.cacheOnLogin(uuid, address).join();
		assertEquals(Optional.of(oldMute), muteCache.getCachedMute(uuid, address).join());

		muteCache.setCachedMute(uuid, address, newMute);
		assertEquals(Optional.of(newMute), muteCache.getCachedMute(uuid, address).join());
	}

	// uncacheOnQuit

	@Test
	public void muteIsNotAvailableAfterQuit() {
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(futuresFactory.completedFuture(Optional.empty()));

		muteCache.cacheOnLogin(uuid, address).join();
		// Player is now logged in

		CentralisedFuture<Optional<Punishment>> databaseQuery = futuresFactory.newIncompleteFuture();
		when(selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE))
				.thenReturn(databaseQuery);

		when(envUserResolver.lookupName(uuid)).thenReturn(futuresFactory.completedFuture(Optional.empty()));
		// Player is now logged out, but mute is still cached
		assertAvailableCacheResult(null);

		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		// Mute is still cached due to grace period
		assertAvailableCacheResult(null);

		time.advanceBy(Duration.ofNanos(GRACE_PERIOD_NANOS));
		time.advanceBy(PURGE_TASK_INTERVAL);
		runScheduledTasks();
		// Player is logged out and mute is purged

		CentralisedFuture<Optional<Punishment>> cachedMute = muteCache.getCachedMute(uuid, address);
		assertFalse(cachedMute.isDone());
		// Complete the query, which allows the request to complete
		databaseQuery.complete(Optional.empty());
		assertTrue(cachedMute.isDone());
		assertEquals(Optional.empty(), cachedMute.join());
	}
}
