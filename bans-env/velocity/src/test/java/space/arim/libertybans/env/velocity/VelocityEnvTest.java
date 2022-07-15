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

package space.arim.libertybans.env.velocity;

import org.junit.jupiter.api.Test;
import space.arim.api.util.testing.InjectableConstructor;
import space.arim.libertybans.core.env.ParallelisedListener;
import space.arim.libertybans.core.env.PlatformListener;

import java.util.Set;

public class VelocityEnvTest {

	@Test
	public void allListenersDeclared() {
		new InjectableConstructor(VelocityEnv.class)
				.verifyParametersContainSubclassesOf(PlatformListener.class, (clazz) -> {
					// Exclude CommandHandler since it is constructed directly
					// Exclude VelocityAsyncListener and ParallelisedListener, which would never be injected
					boolean excluded = Set.of(
							CommandHandler.class, VelocityAsyncListener.class, ParallelisedListener.class
					).contains(clazz);
					return !excluded;
				});
	}
}
