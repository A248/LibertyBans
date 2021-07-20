/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.it.env;

import space.arim.api.env.PlatformHandle;
import space.arim.api.env.PlatformPluginInfo;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

public class QuackHandle implements PlatformHandle {
	
	private final QuackPlatform platform;
	
	QuackHandle(QuackPlatform platform) {
		this.platform = platform;
	}

	@Override
	public PlatformPluginInfo getImplementingPluginInfo() {
		return new PlatformPluginInfo(this, platform);
	}

	@Override
	public FactoryOfTheFuture createFuturesFactory() {
		return new IndifferentFactoryOfTheFuture();
	}

	@Override
	public EnhancedExecutor createEnhancedExecutor() {
		return new SimplifiedEnhancedExecutor() {
			@Override
			public void execute(Runnable command) {
				new Thread(command).start();
			}
		};
	}

	@Override
	public String getPlatformVersion() {
		return "QuackPlatform-SNAPSHOT";
	}

}
