/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.database.pagination;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.NetworkAddress;

import java.nio.ByteBuffer;

final class AddressBorderValue implements BorderValueHandle<NetworkAddress> {
    @Override
    public int len() {
        return 2;
    }

    @Override
    public void writeChatCode(@NonNull NetworkAddress value, @NonNull Write write) {
        byte[] address = value.getRawAddress();
        boolean ipv4 = address.length == 4;
        char leader = ipv4 ? '0' : '1';
        ByteBuffer buffer = ByteBuffer.wrap(address);
        if (ipv4) {
            int integer = buffer.getInt();
            write.writePart(Character.toString(leader));
            write.writePart(Integer.toString(integer, Character.MAX_RADIX));
        } else {
            assert address.length == 16 : "ipv6 consists of 16 bytes";
            long long1 = buffer.getLong();
            long long2 = buffer.getLong();
            write.writePart(leader + Long.toString(long1, Character.MAX_RADIX));
            write.writePart(Long.toString(long2, Character.MAX_RADIX));
        }
    }

    @Override
    public @Nullable NetworkAddress readChatCode(@NonNull Read read) {
        String part1 = read.readPart();
        if (part1.isEmpty()) {
            return null;
        }
        char leader = part1.charAt(0);
        boolean ipv4;
        if (leader == '0') {
            ipv4 = true;
        } else if (leader == '1') {
            ipv4 = false;
        } else {
            return null;
        }
        String part2 = read.readPart();
        byte[] address = new byte[ipv4 ? 4 : 16];
        ByteBuffer buffer = ByteBuffer.wrap(address);
        try {
            if (ipv4) {
                int integer = Integer.parseInt(part2, Character.MAX_RADIX);
                buffer.putInt(integer);
            } else {
                long long1 = Long.parseLong(part1.substring(1), Character.MAX_RADIX);
                long long2 = Long.parseLong(part2, Character.MAX_RADIX);
                buffer.putLong(long1);
                buffer.putLong(long2);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return NetworkAddress.of(address);
    }
}
