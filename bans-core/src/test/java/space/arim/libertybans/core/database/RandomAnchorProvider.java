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

package space.arim.libertybans.core.database;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class RandomAnchorProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        List<KeysetAnchor<Long>> anchors = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int counter = 0; counter < 100; counter++) {
            int page = random.nextInt(Integer.MAX_VALUE) + 1;
            long borderValue = random.nextLong(Long.MAX_VALUE) + 1;
            boolean forwardScroll = random.nextBoolean();
            anchors.add(new KeysetAnchor<>(page, borderValue, forwardScroll));
        }
        return anchors.stream().map(Arguments::of);
    }
}
