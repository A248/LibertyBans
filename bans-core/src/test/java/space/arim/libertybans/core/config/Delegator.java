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
package space.arim.libertybans.core.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

abstract class Delegator<T> implements InvocationHandler {

	private final Class<T> clazz;
	private final T delegate;

	Delegator(Class<T> clazz, T delegate) {
		this.clazz = clazz;
		this.delegate = delegate;
	}

	T proxy() {
		return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, this));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object replacement = replacementFor(delegate, method.getName());
		if (replacement != null) {
			return replacement;
		}
		return method.invoke(delegate, args);
	}

	abstract Object replacementFor(T original, String methodName);

}
