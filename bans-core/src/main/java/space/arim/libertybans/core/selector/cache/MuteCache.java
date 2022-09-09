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
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.Part;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.Optional;
import java.util.UUID;

public interface MuteCache extends Part {

	CentralisedFuture<Optional<Punishment>> getCachedMute(UUID uuid, NetworkAddress address);

	CentralisedFuture<Optional<Component>> getCachedMuteMessage(UUID uuid, NetworkAddress address);

	/**
	 * Fills the cache, as needed, upon player login
	 *
	 * @param uuid the user's uuid
	 * @param address the user's address
	 * @return a future completed when the cache is adequately filled
	 */
	CentralisedFuture<?> cacheOnLogin(UUID uuid, NetworkAddress address);

	void setCachedMute(UUID uuid, NetworkAddress address, Punishment punishment);

	void clearCachedMute(Punishment punishment);

	void clearCachedMute(long id);

}
