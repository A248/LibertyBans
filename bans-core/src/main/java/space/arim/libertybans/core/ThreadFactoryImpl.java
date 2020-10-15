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

import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

class ThreadFactoryImpl implements ThreadFactory {

	private final String prefix;
	private int threadId;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	ThreadFactoryImpl(String prefix) {
		this.prefix = prefix;
	}
	
	private synchronized int nextId() {
		return ++threadId;
	}
	
	@Override
	public Thread newThread(Runnable r) {
		String name = prefix + nextId();
		logger.debug("Spawning new thread {}", name);
		return new Thread(r, name);
	}
	
}
