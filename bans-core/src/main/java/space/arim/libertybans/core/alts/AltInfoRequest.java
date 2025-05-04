/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.core.alts;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.database.pagination.InstantThenUUID;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;

import java.util.Objects;
import java.util.UUID;

public record AltInfoRequest(UUID uuid, NetworkAddress address, WhichAlts filter,
                             boolean oldestFirst, int pageSize,
                             KeysetAnchor<InstantThenUUID> pageAnchor, int skipCount) {

    /**
     * Creates a retrieval request without a page anchor, meaning start at the first page.
     *
     * @param uuid        the user ID
     * @param address     their address
     * @param filter      which alts to filter for
     * @param oldestFirst whether to sort oldest first
     * @param pageSize    the maximum number to retrieve, or -1 for unlimited
     */
    public AltInfoRequest(UUID uuid, NetworkAddress address, WhichAlts filter, boolean oldestFirst, int pageSize) {
        this(uuid, address, filter, oldestFirst, pageSize, KeysetAnchor.unset(), 0);
    }

    /**
     * Creates a retrieval request.
     *
     * @param uuid                the user ID
     * @param address             their address
     * @param filter              which alts to filter for
     * @param oldestFirst         whether to sort oldest first
     * @param pageSize            the maximum number to retrieve, for -1 for unlimited
     * @param pageAnchor          the numeric code after which to retrieve alts, 0 if unset
     * @param skipCount           how many alts to skip
     */
    public AltInfoRequest {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(address);
        Objects.requireNonNull(filter);
    }
}
