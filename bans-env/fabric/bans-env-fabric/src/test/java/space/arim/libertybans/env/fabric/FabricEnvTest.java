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

import org.junit.jupiter.api.Test;
import space.arim.api.util.testing.InjectableConstructor;
import space.arim.libertybans.core.env.PlatformListener;

public class FabricEnvTest {
    @Test
    public void allListenersDeclared() {
        InjectableConstructor injectableConstructor =
                new InjectableConstructor(FabricEnv.class);
        injectableConstructor.verifyParametersContainSubclassesOf(PlatformListener.class, (clazz) -> {
                    // Use only classes in our package or subpackages
                    return clazz.getPackageName().startsWith(getClass().getPackageName())
                            && !clazz.equals(FabricListener.class);
                });
        injectableConstructor.verifyParametersContainSubclassesOf(FabricListener.class);
    }
}
