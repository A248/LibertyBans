/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.env;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import space.arim.omnibus.resourcer.ResourceHook;
import space.arim.omnibus.resourcer.ResourceInfo;
import space.arim.omnibus.resourcer.Resourcer;
import space.arim.omnibus.resourcer.ShutdownHandler;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.PlatformPluginInfo;
import space.arim.api.env.PlatformType;
import space.arim.api.env.annote.PlatformCommandSender;
import space.arim.api.env.annote.PlatformPlayer;
import space.arim.api.env.realexecutor.RealExecutorFinder;

import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;

public class QuackHandle implements PlatformHandle {
	
	private final QuackPlatform platform;
	
	QuackHandle(QuackPlatform platform) {
		this.platform = platform;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> ResourceHook<T> hookPlatformResource(Resourcer resourcer, Class<T> resourceClass) {
		if (resourceClass == EnhancedExecutor.class) {
			return resourcer.hookUsage(resourceClass, () -> {
				EnhancedExecutor ee = new SimplifiedEnhancedExecutor() {
					@Override
					public void execute(Runnable command) {
						new Thread(command).start();
					}
				};
				return new ResourceInfo<T>("QuackPlatformEnhancedExecutor", (T) ee, ShutdownHandler.none());
			});
		}
		if (resourceClass == FactoryOfTheFuture.class) {
			return resourcer.hookUsage(resourceClass, () -> {
				return new ResourceInfo<T>("QuackPlatformFuturesFactory",
						(T) new IndifferentFactoryOfTheFuture(), ShutdownHandler.none());
			});
		}
		throw new IllegalArgumentException();
	}

	@Override
	public void sendMessage(@PlatformCommandSender Object recipient, SendableMessage message) {
		((QuackPlayer) recipient).sendMessage(message);
	}

	@Override
	public void disconnectUser(@PlatformPlayer Object user, SendableMessage reason) {
		((QuackPlayer) user).kickPlayer(reason);
	}

	@Override
	public RealExecutorFinder getRealExecutorFinder() {
		return new RealExecutorFinder() {

			@Override
			public Executor findExecutor(Consumer<Exception> exceptionHandler) {
				return null;
			}
			
		};
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@Override
	public PlatformType getPlatformType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PlatformPluginInfo getImplementingPluginInfo() {
		return new PlatformPluginInfo(this, platform);
	}

}
