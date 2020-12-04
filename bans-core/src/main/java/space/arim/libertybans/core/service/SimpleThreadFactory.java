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
package space.arim.libertybans.core.service;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

public class SimpleThreadFactory implements ThreadFactory {

	private final String prefix;
	private final AtomicInteger threadId = new AtomicInteger();
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private SimpleThreadFactory(String prefix) {
		this.prefix = prefix;
	}
	
	public static ThreadFactory create(String componentName) {
		return new SimpleThreadFactory("LibertyBans-" + componentName + "-");
	}
	
	private int nextId() {
		return threadId.incrementAndGet();
	}
	
	@Override
	public Thread newThread(Runnable r) {
		String name = prefix + nextId();
		logger.debug("Spawning new thread {}", name);
		return new Thread(r, name);
	}
	
}
