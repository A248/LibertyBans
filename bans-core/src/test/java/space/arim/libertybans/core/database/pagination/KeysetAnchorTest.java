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

package space.arim.libertybans.core.database.pagination;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import space.arim.libertybans.core.database.RandomAnchorProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeysetAnchorTest {

    @ParameterizedTest
    @ArgumentsSource(RandomAnchorProvider.class)
    public void toAndFromCode(KeysetAnchor<Long> anchor) {
        String toCode = anchor.chatCode(new LongBorderValue());
        var reconstituted = new KeysetAnchor.Build<>(new LongBorderValue()).fromCode(toCode);
        assertEquals(Optional.of(anchor), reconstituted, "Got code " + toCode);
    }
}
