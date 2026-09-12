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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AddressBorderValueTest {

    private final BorderValueHandle<NetworkAddress> handle = new AddressBorderValue();

    static NetworkAddress sizedAddress(int size) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        byte[] address = new byte[size];
        random.nextBytes(address);
        return NetworkAddress.of(address);
    }

    private void roundTrip(NetworkAddress address) {
        List<String> parts = new ArrayList<>();
        handle.writeChatCode(address, parts::add);
        assertEquals(parts.size(), handle.len());
        NetworkAddress remade = handle.readChatCode(new BorderValueHandle.Read() {
            private int idx;
            @Override
            public String readPart() {
                return parts.get(idx++);
            }
        });
        assertEquals(address, remade);
    }

    @RepeatedTest(4)
    public void roundTripIpv4() {
        roundTrip(sizedAddress(4));
    }

    @RepeatedTest(4)
    public void roundTripIpv6() {
        roundTrip(sizedAddress(16));
    }
}
