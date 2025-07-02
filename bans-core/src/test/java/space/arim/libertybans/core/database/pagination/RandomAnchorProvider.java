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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class RandomAnchorProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

        // Handle either KeysetAnchor<Long> or KeysetAnchor<UUID> or KeysetAnchor<InstantThenUUID>
        Type parameter = context.getRequiredTestMethod().getGenericParameterTypes()[0];
        if (!(parameter instanceof ParameterizedType)) {
            throw new IllegalStateException();
        }
        Class<?> borderValueType = (Class<?>) ((ParameterizedType) parameter).getActualTypeArguments()[0];
        List<Object> anchors = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int counter = 0; counter < 30; counter++) {

            int page = random.nextInt(Integer.MAX_VALUE) + 1;
            boolean forwardScroll = random.nextBoolean();
            Object borderValue;
            if (borderValueType.equals(Long.class)) {
                borderValue = random.nextLong(Long.MAX_VALUE) + 1;
            } else if (borderValueType.equals(UUID.class)) {
                borderValue = new UUID(
                        random.nextLong(Long.MAX_VALUE) + 1,
                        random.nextLong(Long.MAX_VALUE) + 1
                );
            } else if (borderValueType.equals(InstantThenUUID.class)) {
                borderValue = new InstantThenUUID(
                        Instant.ofEpochSecond(random.nextLong(Instant.MAX.getEpochSecond()) + 1),
                        new UUID(
                                random.nextLong(Long.MAX_VALUE) + 1,
                                random.nextLong(Long.MAX_VALUE) + 1
                        )
                );
            } else {
                throw new IllegalStateException("Not a valid border value type " + borderValueType);
            }
            anchors.add(new KeysetAnchor<>(page, borderValue, forwardScroll));
        }
        return anchors.stream().map(Arguments::of);
    }
}
