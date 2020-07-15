/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class Instantiator {

	private final Class<?> clazz;
	
	public Instantiator(String clazzName, ClassLoader loader) throws ClassNotFoundException {
		clazz = Class.forName(clazzName, true, loader);
	}
	
	public <T> BaseEnvironment invoke(Class<T> parameterType, T parameter) throws ReflectiveOperationException, IllegalArgumentException, SecurityException {
		return (BaseEnvironment) clazz.getDeclaredConstructor(parameterType).newInstance(parameter);
	}
	
	/**
	 * Creates a reasonable thread pool for downloading dependencies. <br>
	 * <br>
	 * If there are enough threads in the ForkJoinPool's commonPool it is used, else a typical thread pool
	 * is returned, which must be shutdown.
	 * 
	 * @return a thread pool for downloading dependencies
	 */
	public static ExecutorService createReasonableExecutor() {
		int minParallelism = 5;
		int commonParallelism = ForkJoinPool.getCommonPoolParallelism();
		if (commonParallelism >= minParallelism) {
			return ForkJoinPool.commonPool();
		}
		return Executors.newFixedThreadPool(minParallelism);
	}
	
}
