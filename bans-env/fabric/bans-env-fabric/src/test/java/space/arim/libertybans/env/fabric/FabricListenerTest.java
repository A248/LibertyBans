/*
 * LibertyBans-fabric
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans-fabric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibertyBans-fabric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibertyBans-fabric. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */

package space.arim.libertybans.env.fabric;

import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import space.arim.api.util.testing.SubClassesOf;
import space.arim.libertybans.core.env.PlatformListener;

import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FabricListenerTest {
    @Test
    public void allSingletons() {
        Set<Class<?>> classes = new SubClassesOf(PlatformListener.class)
                .scan(getClass().getModule(), (clazz) -> clazz.getPackage().equals(getClass().getPackage()));
        for (Class<?> listener : classes) {
            if (Modifier.isAbstract(listener.getModifiers())) {
                continue;
            }
            assertNotNull(listener.getAnnotation(Singleton.class), () -> "Listener " + listener + " must be @Singleton");
        }
    }
}
