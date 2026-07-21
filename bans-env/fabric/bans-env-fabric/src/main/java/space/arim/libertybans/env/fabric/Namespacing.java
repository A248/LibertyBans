/*
 * LibertyBans-fabric
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans-fabric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibertyBans-fabric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibertyBans-fabric. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */

package space.arim.libertybans.env.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;

final class Namespacing {

    private static final String OWN_NAMESPACE = "libertybans";

    private Namespacing() {}

    static Identifier makeIdentifier(String namespace, String value) {
        Identifier identifier = Identifier.tryBuild(namespace, value);
        if (identifier == null) {
            throw new IllegalArgumentException("Illegal identifier: " + namespace + " + " + value);
        }
        return identifier;
    }

    static Identifier ownIdentifier(String value) {
        return makeIdentifier(OWN_NAMESPACE, value);
    }

    static Permission parsePermission(String value) {
        if (!value.startsWith(OWN_NAMESPACE)) {
            throw new IllegalArgumentException("Value must be in our namespace: " + value);
        }
        String afterNamespace = value.substring(OWN_NAMESPACE.length() + 1);
        return new Permission.Atom(ownIdentifier(afterNamespace));
    }
}
