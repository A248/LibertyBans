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

package space.arim.libertybans.bootstrap.classload;

public interface ClassLoadGuard {

    Class<?> delegateLoadClass(String className, boolean resolve, Destination destination)
            throws ClassNotFoundException;

    default AttachableClassLoader makeClassLoader(String classLoaderName, ClassLoader parentLoader) {
        return new AttachableClassLoader(classLoaderName, new GuardedClassLoader(parentLoader, this));
    }

    static ClassLoadGuard passThrough() {
        class PassThrough implements ClassLoadGuard {

            @Override
            public Class<?> delegateLoadClass(String className, boolean resolve, Destination destination) throws ClassNotFoundException {
                return destination.load(className, resolve);
            }

            @Override
            public AttachableClassLoader makeClassLoader(String classLoaderName, ClassLoader parentLoader) {
                return new AttachableClassLoader(classLoaderName, parentLoader);
            }
        }
        return new PassThrough();
    }

    interface Destination {

        Class<?> load(String className, boolean resolve) throws ClassNotFoundException;
    }
}
