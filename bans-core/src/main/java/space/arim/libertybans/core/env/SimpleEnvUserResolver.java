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

package space.arim.libertybans.core.env;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An easy way to implement the user resolver assuming all base operations require
 * the same thread context.
 * 
 */
public abstract class SimpleEnvUserResolver implements EnvUserResolver {

	/**
	 * Performs a lookup in the thread context of choice
	 *
	 * @param <U> the lookup return type
	 * @param rootImplementation the implementation of the particular lookup
	 * @return a future completed when the lookup is complete
	 */
	protected abstract <U> CentralisedFuture<U> performLookup(Supplier<U> rootImplementation);

	@Override
	public final CentralisedFuture<Optional<UUID>> lookupUUID(String name) {
		return performLookup(() -> lookupUUID0(name));
	}

	@Override
	public final CentralisedFuture<Optional<String>> lookupName(UUID uuid) {
		return performLookup(() -> lookupName0(uuid));
	}

	@Override
	public final CentralisedFuture<Optional<InetAddress>> lookupAddress(String name) {
		return performLookup(() -> lookupAddress0(name));
	}

	@Override
	public final CentralisedFuture<Optional<UUIDAndAddress>> lookupPlayer(String name) {
		return performLookup(() -> lookupPlayer0(name));
	}

	@Override
	public final CentralisedFuture<Optional<InetAddress>> lookupCurrentAddress(UUID uuid) {
		return performLookup(() -> lookupCurrentAddress0(uuid));
	}

	protected abstract Optional<UUID> lookupUUID0(String name);

	protected abstract Optional<String> lookupName0(UUID uuid);

	protected abstract Optional<InetAddress> lookupAddress0(String name);

	protected abstract Optional<UUIDAndAddress> lookupPlayer0(String name);

	protected abstract Optional<InetAddress> lookupCurrentAddress0(UUID uuid);

}
