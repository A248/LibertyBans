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

import space.arim.libertybans.bootstrap.ProtectedLibrary;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public final class LibraryProtection implements ClassLoadGuard {

    private static final ClassNotFoundException FAILED_FILTER;

    static {
        FAILED_FILTER = new ClassNotFoundException(
                "Class is filtered from the eyes of child classloaders");
    }

    private final ProtectedLibrary[] protectedLibraries;
    private final ClassLoadGuard next;

    public LibraryProtection(Set<ProtectedLibrary> protectedLibraries, ClassLoadGuard next) {
        this.protectedLibraries = protectedLibraries.toArray(new ProtectedLibrary[] {});
        this.next = Objects.requireNonNull(next);
    }

    @Override
    public Class<?> delegateLoadClass(String className, boolean resolve, Destination destination) throws ClassNotFoundException {
        for (ProtectedLibrary protectedLibrary : protectedLibraries) {
            if (className.startsWith(protectedLibrary.basePackage())) {
                throw FAILED_FILTER;
            }
        }
        return next.delegateLoadClass(className, resolve, destination);
    }

    @Override
    public String toString() {
        return "LibraryProtection{" +
                "protectedLibraries=" + Arrays.toString(protectedLibraries) +
                ", next=" + next +
                '}';
    }
}
