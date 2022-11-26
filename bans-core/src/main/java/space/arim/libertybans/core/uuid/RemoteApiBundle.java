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

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.util.web.HttpAshconApi;
import space.arim.api.util.web.HttpMcHeadsApi;
import space.arim.api.util.web.HttpMojangApi;
import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameUUIDApi;

import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;

public class RemoteApiBundle {

	private final List<RemoteNameUUIDApi> remotes;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	public RemoteApiBundle(List<RemoteNameUUIDApi> remotes) {
		this.remotes = List.copyOf(remotes);
	}

	private <T> T unboxResult(RemoteNameUUIDApi remoteApi, RemoteApiResult<T> remoteApiResult) {
		return switch (remoteApiResult.getResultType()) {
			case FOUND, NOT_FOUND -> remoteApiResult.getValue();
			case RATE_LIMITED, ERROR -> {
				Exception ex = remoteApiResult.getException();
				if (ex == null) {
					logger.warn("Request for name to remote web API {} failed", remoteApi);
				} else {
					logger.warn("Request for name to remote web API {} failed", remoteApi, ex);
				}
				yield null;
			}
		};
	}
	
	<T> CompletableFuture<T> lookup(Function<RemoteNameUUIDApi, CompletableFuture<RemoteApiResult<T>>> intermediateResultFunction) {
		CompletableFuture<T> future = null;
		for (RemoteNameUUIDApi remoteApi : remotes) {

			if (future == null) {
				future = intermediateResultFunction.apply(remoteApi)
						.thenApply((remoteApiResult) -> unboxResult(remoteApi, remoteApiResult));
			} else {
				future = future.thenCompose((result) -> {
					if (result != null) {
						return CompletableFuture.completedFuture(result);
					}
					return intermediateResultFunction.apply(remoteApi)
							.thenApply((remoteApiResult) -> unboxResult(remoteApi, remoteApiResult));
				});
			}
		}
		if (future == null) {
			return CompletableFuture.completedFuture(null);
		}
		return future;
	}
	
	private enum RemoteType {
		ASHCON(HttpAshconApi::create),
		MCHEADS(HttpMcHeadsApi::create),
		MOJANG(HttpMojangApi::create);
		
		final Function<HttpClient, RemoteNameUUIDApi> creator;

		RemoteType(Function<HttpClient, RemoteNameUUIDApi> creator) {
			this.creator = creator;
		}

		static RemoteType fromInstance(RemoteNameUUIDApi remote) {
			if (remote instanceof HttpAshconApi) {
				return ASHCON;
			}
			if (remote instanceof HttpMcHeadsApi) {
				return MCHEADS;
			}
			if (remote instanceof HttpMojangApi) {
				return MOJANG;
			}
			throw new IllegalArgumentException("Unknown implementing instance of " + remote);
		}
	}

	public static final class SerialiserImpl implements ValueSerialiser<RemoteApiBundle> {

		@Override
		public Class<RemoteApiBundle> getTargetClass() {
			return RemoteApiBundle.class;
		}

		@Override
		public RemoteApiBundle deserialise(FlexibleType flexibleType) throws BadValueException {
			HttpClient httpClient = HttpClient.newHttpClient();
			List<RemoteNameUUIDApi> remotes = flexibleType.getList((flexibleElement) -> {
				RemoteType remoteType = flexibleElement.getObject(RemoteType.class);
				return remoteType.creator.apply(httpClient);
			});
			return new RemoteApiBundle(remotes);
		}

		@Override
		public Object serialise(RemoteApiBundle value, Decomposer decomposer) {
			List<String> result = new ArrayList<>();
			for (RemoteNameUUIDApi remote : value.remotes) {
				result.add(RemoteType.fromInstance(remote).name());
			}
			return result;
		}

	}

}
