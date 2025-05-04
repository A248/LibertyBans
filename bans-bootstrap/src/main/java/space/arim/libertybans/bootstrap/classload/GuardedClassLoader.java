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

import java.util.Objects;

public final class GuardedClassLoader extends ClassLoader {

    private static final ClassNotFoundException DOES_NOT_LOAD_CLASSES;

    static {
        ClassLoader.registerAsParallelCapable();
        DOES_NOT_LOAD_CLASSES = new ClassNotFoundException(
                GuardedClassLoader.class.getName() + " does not itself load classes"
        );
    }

    private final ClassLoadGuard guard;

    public GuardedClassLoader(ClassLoader parent, ClassLoadGuard guard) {
        super(parent);
        this.guard = Objects.requireNonNull(guard);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        return guard.delegateLoadClass(className, resolve, super::loadClass);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        throw DOES_NOT_LOAD_CLASSES;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
                "guard=" + guard +
                ", parent=" + getParent() +
                '}';
    }
}
