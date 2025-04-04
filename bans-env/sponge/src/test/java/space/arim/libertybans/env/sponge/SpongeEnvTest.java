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

package space.arim.libertybans.env.sponge;

import org.junit.jupiter.api.Test;
import space.arim.api.util.testing.InjectableConstructor;
import space.arim.libertybans.core.env.PlatformListener;

public class SpongeEnvTest {

	@Test
	public void allListenersDeclared() {
		new InjectableConstructor(SpongeEnv.class)
				.verifyParametersContainSubclassesOf(PlatformListener.class, (clazz) -> {
					// Exclude CommandHandler which is constructed directly
					// Use only classes in our package or subpackages
					// Exclude enclosed, anonymous, or local classes
					return !clazz.equals(CommandHandler.class)
							&& clazz.getPackageName().startsWith(getClass().getPackageName())
							&& clazz.getEnclosingClass() == null
							&& !clazz.isAnonymousClass()
							&& !clazz.isLocalClass();
				});
	}

}
