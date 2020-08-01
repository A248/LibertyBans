/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core;

import java.time.Duration;

import space.arim.omnibus.resourcer.ResourceHook;
import space.arim.omnibus.resourcer.Resourcer;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.api.env.PlatformHandle;

public class Resources implements Part {

	private final LibertyBansCore core;
	
	private volatile HookBundle bundle;
	
	Resources(LibertyBansCore core) {
		this.core = core;
	}
	
	FactoryOfTheFuture getFuturesFactory() {
		return bundle.futuresFactory.getResource();
	}
	
	public EnhancedExecutor getEnhancedExecutor() {
		return bundle.enhancedExecutor.getResource();
	}
	
	private Resourcer getResourcer() {
		return core.getOmnibus().getResourcer();
	}
	
	private static class HookBundle {
		final ResourceHook<FactoryOfTheFuture> futuresFactory;
		final ResourceHook<EnhancedExecutor> enhancedExecutor;
		
		HookBundle(ResourceHook<FactoryOfTheFuture> futuresFactory, ResourceHook<EnhancedExecutor> enhancedExecutor) {
			this.futuresFactory = futuresFactory;
			this.enhancedExecutor = enhancedExecutor;
		}
	}

	@Override
	public void startup() {
		Resourcer resourcer = getResourcer();
		PlatformHandle handle = core.getEnvironment().getPlatformHandle();
		ResourceHook<FactoryOfTheFuture> futuresFactory = handle.hookPlatformResource(resourcer, FactoryOfTheFuture.class);
		ResourceHook<EnhancedExecutor> enhancedExecutor = handle.hookPlatformResource(resourcer, EnhancedExecutor.class);
		bundle = new HookBundle(futuresFactory, enhancedExecutor);
	}
	
	@Override
	public void restart() {}

	@Override
	public void shutdown() {
		final HookBundle bundle = this.bundle;
		unhookLater(bundle);
		this.bundle = null;
	}
	
	private void unhookLater(HookBundle bundle) {
		Resourcer resourcer = getResourcer();
		/*
		 * 5 seconds should be enough time
		 */
		Duration unhookTime = Duration.ofSeconds(5L);
		bundle.enhancedExecutor.getResource().scheduleOnce(() -> {
			resourcer.unhookUsage(bundle.futuresFactory);
			resourcer.unhookUsage(bundle.enhancedExecutor);
		}, unhookTime);
	}
	
}
