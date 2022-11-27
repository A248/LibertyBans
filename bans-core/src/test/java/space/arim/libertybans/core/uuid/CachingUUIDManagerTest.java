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

package space.arim.libertybans.core.uuid;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CachingUUIDManagerTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final Configs configs;
	private final NameValidator nameValidator;
	private final EnvUserResolver envUserResolver;
	private final QueryingImpl queryingImpl;
	private final Time time;

	private final UUIDManager uuidManager;

	private final UUID uuid = UUIDUtil.fromShortString("ed5f12cd600745d9a4b9940524ddaecf");
	private final String name = "A248";
	private final NetworkAddress address = NetworkAddress.of(new byte[4]);

	public CachingUUIDManagerTest(@Mock Configs configs, @Mock NameValidator nameValidator,
								  @Mock EnvUserResolver envUserResolver, @Mock QueryingImpl queryingImpl,
								  @Mock Time time) {
		this.configs = configs;
		this.nameValidator = nameValidator;
		this.envUserResolver = envUserResolver;
		this.queryingImpl = queryingImpl;
		this.time = time;

		uuidManager = new CachingUUIDManager(configs, futuresFactory, envUserResolver, queryingImpl, nameValidator, time);
	}

	@BeforeEach
	public void setup() {
		MainConfig mainConfig = mock(MainConfig.class);
		UUIDResolutionConfig uuidResolution = mock(UUIDResolutionConfig.class);
		lenient().when(configs.getMainConfig()).thenReturn(mainConfig);
		lenient().when(mainConfig.uuidResolution()).thenReturn(uuidResolution);

		when(time.toCaffeineTicker()).thenReturn(Ticker.disabledTicker());

		uuidManager.startup();
		lenient().when(nameValidator.validateNameArgument(name)).thenReturn(true);

		lenient().when(envUserResolver.lookupUUID(any())).thenReturn(completedFuture(Optional.empty()));
		lenient().when(envUserResolver.lookupName(any())).thenReturn(completedFuture(Optional.empty()));
		lenient().when(envUserResolver.lookupAddress(any())).thenReturn(completedFuture(Optional.empty()));
		lenient().when(envUserResolver.lookupPlayer(any())).thenReturn(completedFuture(Optional.empty()));
	}

	private UUID lookupUUID(String name) {
		return uuidManager.lookupUUID(name).join().orElse(null);
	}

	private UUID lookupUUIDExact(String name) {
		return uuidManager.lookupUUIDFromExactName(name).join().orElse(null);
	}

	private String lookupName(UUID uuid) {
		return uuidManager.lookupName(uuid).join().orElse(null);
	}

	private NetworkAddress lookupAddress(String name) {
		return uuidManager.lookupAddress(name).join();
	}

	private UUIDAndAddress lookupPlayer(String name) {
		return uuidManager.lookupPlayer(name).join().orElse(null);
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	@Test
	public void resolveUUIDFromEnvironment() {
		when(envUserResolver.lookupUUID(name)).thenReturn(
				completedFuture(Optional.of(uuid)), completedFuture(Optional.empty()));

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUID(name), "uuid should be cached");
		assertEquals(uuid, lookupUUIDExact(name));

		verify(envUserResolver).lookupUUID(name);
	}

	@Test
	public void resolveNameFromEnvironment() {
		when(envUserResolver.lookupName(uuid)).thenReturn(
				completedFuture(Optional.of(name)), completedFuture(Optional.empty()));

		assertEquals(name, lookupName(uuid));
		assertEquals(name, lookupName(uuid), "Name should be cached");

		verify(envUserResolver).lookupName(uuid);
	}

	@Test
	public void resolveAddressFromEnvironment() {
		when(envUserResolver.lookupAddress(name)).thenReturn(completedFuture(Optional.of(address.toInetAddress())));

		assertEquals(address, lookupAddress(name));

		verify(envUserResolver).lookupAddress(name);
	}

	@Test
	public void resolvePlayerFromEnvironment() {
		UUIDAndAddress userDetails = new UUIDAndAddress(uuid, address);
		when(envUserResolver.lookupPlayer(name)).thenReturn(completedFuture(Optional.of(userDetails)));

		assertEquals(userDetails, lookupPlayer(name));

		verify(envUserResolver).lookupPlayer(name);
	}

	@Test
	public void resolveUUIDQueried() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(uuid), completedFuture(null));

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUID(name), "uuid should be cached");
		assertEquals(uuid, lookupUUIDExact(name));

		verify(queryingImpl).resolve(name);
	}

	@Test
	public void resolveNameQueried() {
		when(queryingImpl.resolve(uuid)).thenReturn(completedFuture(name), completedFuture(null));

		assertEquals(name, lookupName(uuid));
		assertEquals(name, lookupName(uuid), "Name should be cached");

		verify(queryingImpl).resolve(uuid);
	}

	@Test
	public void resolveAddressQueried() {
		when(queryingImpl.resolveAddress(name)).thenReturn(completedFuture(address));

		assertEquals(address, lookupAddress(name));

		verify(queryingImpl).resolveAddress(name);
	}

	@Test
	public void resolvePlayerQueried() {
		UUIDAndAddress userDetails = new UUIDAndAddress(uuid, address);
		when(queryingImpl.resolvePlayer(name)).thenReturn(completedFuture(userDetails));

		assertEquals(userDetails, lookupPlayer(name));

		verify(queryingImpl).resolvePlayer(name);
	}

	private void mockConfig(ServerType serverType, RemoteApiBundle remoteApiBundle) {
		MainConfig mainConfig = mock(MainConfig.class);
		UUIDResolutionConfig uuidResolution = mock(UUIDResolutionConfig.class);
		when(configs.getMainConfig()).thenReturn(mainConfig);
		when(mainConfig.uuidResolution()).thenReturn(uuidResolution);
		when(uuidResolution.serverType()).thenReturn(serverType);
		lenient().when(uuidResolution.remoteApis()).thenReturn(remoteApiBundle);
	}

	@Test
	public void resolveUUIDWebLookup() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(null));

		RemoteApiBundle remoteApiBundle = mock(RemoteApiBundle.class);
		mockConfig(ServerType.ONLINE, remoteApiBundle);
		when(remoteApiBundle.lookup(any())).thenReturn(
				completedFuture(uuid), completedFuture(null));
		when(nameValidator.isVanillaName(name)).thenReturn(true);

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUID(name), "uuid should be cached");
		assertEquals(uuid, lookupUUIDExact(name));
	}

	@Test
	public void resolveNameWebLookup() {
		when(queryingImpl.resolve(uuid)).thenReturn(completedFuture(null));

		RemoteApiBundle remoteApiBundle = mock(RemoteApiBundle.class);
		mockConfig(ServerType.ONLINE, remoteApiBundle);
		when(remoteApiBundle.lookup(any())).thenReturn(
				completedFuture(name), completedFuture(null));
		when(nameValidator.isVanillaUUID(uuid)).thenReturn(true);

		assertEquals(name, lookupName(uuid));
		assertEquals(name, lookupName(uuid), "Name should be cached");
	}

	@Test
	public void resolveBedrockUUIDWebLookup() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(null));
		when(nameValidator.isVanillaName(name)).thenReturn(false);

		assertNull(lookupUUID(name));
		assertNull(lookupUUIDExact(name));
	}

	@Test
	public void resolveBedrockNameWebLookup() {
		when(queryingImpl.resolve(uuid)).thenReturn(completedFuture(null));
		when(nameValidator.isVanillaUUID(uuid)).thenReturn(false);

		assertNull(lookupName(uuid));
	}

	@Test
	public void resolveUUIDComputeOffline() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(null));

		RemoteApiBundle remoteApiBundle = mock(RemoteApiBundle.class);
		mockConfig(ServerType.OFFLINE, remoteApiBundle);

		UUID uuid = OfflineUUID.computeOfflineUuid(name);
		assertNull(lookupUUID(name), "Inexact lookup cannot compute offline uuid");
		assertEquals(uuid, lookupUUIDExact(name));
		assertEquals(uuid, lookupUUID(name), "uuid should be cached");
	}

	@Test
	public void resolveUUIDExplicitlyCached() {
		uuidManager.addCache(uuid, name);

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUIDExact(name));
	}

	@Test
	public void resolveNameExplicitlyCached() {
		uuidManager.addCache(uuid, name);

		assertEquals(name, lookupName(uuid));
	}

	@Test
	public void resolveNameNotFound() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(null));
		when(queryingImpl.resolveAddress(name)).thenReturn(completedFuture(null));
		when(queryingImpl.resolvePlayer(name)).thenReturn(completedFuture(null));
		assertNull(lookupUUID(name));
		assertNull(lookupUUIDExact(name));
		assertNull(lookupAddress(name));
		assertNull(lookupPlayer(name));
	}

	@Test
	public void resolveUUIDNotFound() {
		when(queryingImpl.resolve(uuid)).thenReturn(completedFuture(null));
		assertNull(lookupName(uuid));
	}

	@Test
	public void badName() {
		String badName = "lol_haha_dead";
		when(nameValidator.validateNameArgument(badName)).thenReturn(false);

		assertNull(lookupUUID(badName));
		assertNull(lookupUUIDExact(badName));
		assertNull(lookupAddress(badName));
		assertNull(lookupPlayer(badName));

		verify(nameValidator, times(4)).validateNameArgument(badName);
	}

}
