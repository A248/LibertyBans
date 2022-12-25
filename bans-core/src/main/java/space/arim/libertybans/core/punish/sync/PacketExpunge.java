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

package space.arim.libertybans.core.punish.sync;

import java.io.IOException;

public final class PacketExpunge implements SynchronizationPacket {

	final long id;

	static final byte PACKET_ID = (byte) 2;

	public PacketExpunge(long id) {
		this.id = id;
	}

	@Override
	public byte packetId() {
		return PACKET_ID;
	}

	@Override
	public void writeTo(ProtocolOutputStream output) throws IOException {
		output.writeLong(id);
	}

	static PacketExpunge readFrom(ProtocolInputStream input) throws IOException {
		long id = input.readLong();
		return new PacketExpunge(id);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PacketExpunge that = (PacketExpunge) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return (int) (id ^ (id >>> 32));
	}

	@Override
	public String toString() {
		return "PacketExpunge{" +
				"id=" + id +
				'}';
	}

}
