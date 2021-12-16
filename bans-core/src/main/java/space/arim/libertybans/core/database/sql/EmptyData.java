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

package space.arim.libertybans.core.database.sql;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.omnibus.util.UUIDUtil;

import java.util.UUID;

public final class EmptyData {

	public static final UUID UUID = new UUID(0, 0);
	public static final byte[] UUID_BYTES = UUIDUtil.toByteArray(UUID);
	public static final String UUID_SHORT_STRING = UUIDUtil.toShortString(UUID);

	public static final NetworkAddress ADDRESS = NetworkAddress.of(new byte[4]);

	private EmptyData() {}
}
