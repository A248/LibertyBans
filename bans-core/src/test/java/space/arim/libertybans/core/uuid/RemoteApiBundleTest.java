/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameUUIDApi;
import space.arim.omnibus.util.UUIDUtil;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RemoteApiBundleTest {

	private final UUID uuid = UUIDUtil.fromShortString("ed5f12cd600745d9a4b9940524ddaecf");
	private final String name = "A248";

	private <T> CompletableFuture<RemoteApiResult<T>> completedResult(T value) {
		return CompletableFuture.completedFuture(RemoteApiResult.found(value));
	}

	private <T> CompletableFuture<RemoteApiResult<T>> emptyResult() {
		return CompletableFuture.completedFuture(RemoteApiResult.notFound());
	}

	@Test
	public void empty() {
		RemoteApiBundle remoteApiBundle = new RemoteApiBundle(List.of());
		assertNull(remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());
	}

	@Test
	public void oneResolver() {
		RemoteNameUUIDApi RemoteNameUUIDApi = mock(RemoteNameUUIDApi.class);
		when(RemoteNameUUIDApi.lookupUUID(name)).thenReturn(completedResult(uuid));
		when(RemoteNameUUIDApi.lookupName(uuid)).thenReturn(completedResult(name));
		RemoteApiBundle remoteApiBundle = new RemoteApiBundle(List.of(RemoteNameUUIDApi));

		assertEquals(uuid, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupUUID(name)).join());
		assertEquals(name, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());

		verify(RemoteNameUUIDApi).lookupUUID(name);
		verify(RemoteNameUUIDApi).lookupName(uuid);
	}

	@Test
	public void multipleResolvers() {
		// One of the objectives here is to ensure remote APIs are not sent a burst of requests.
		// Specifically, if the first remote API finds an answer, don't query the rest.
		RemoteNameUUIDApi inconsistentRemoteApi = mock(RemoteNameUUIDApi.class);
		when(inconsistentRemoteApi.lookupUUID(name)).thenReturn(completedResult(uuid), emptyResult());
		when(inconsistentRemoteApi.lookupName(uuid)).thenReturn(completedResult(name), emptyResult());
		RemoteNameUUIDApi consistentRemoteApi = mock(RemoteNameUUIDApi.class);
		when(consistentRemoteApi.lookupUUID(name)).thenReturn(completedResult(uuid), completedResult(uuid));
		when(consistentRemoteApi.lookupName(uuid)).thenReturn(completedResult(name), completedResult(name));
		RemoteApiBundle remoteApiBundle = new RemoteApiBundle(List.of(inconsistentRemoteApi, consistentRemoteApi));

		assertEquals(uuid, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupUUID(name)).join());
		assertEquals(name, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());
		assertEquals(uuid, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupUUID(name)).join());
		assertEquals(name, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());

		verify(inconsistentRemoteApi, times(2)).lookupUUID(name);
		verify(inconsistentRemoteApi, times(2)).lookupName(uuid);
		verify(consistentRemoteApi).lookupUUID(name);
		verify(consistentRemoteApi).lookupName(uuid);
	}
}
