/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.commands;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class PrependedCommandPackage implements CommandPackage {

    private final String first;
    private final CommandPackage remainder;
    private boolean usedFirst;

    public PrependedCommandPackage(String first, CommandPackage remainder) {
        this.first = first;
        this.remainder = remainder;
    }

    @Override
    public String next() {
        if (usedFirst) {
            return remainder.next();
        }
        usedFirst = true;
        return first;
    }

    @Override
    public String peek() {
        return usedFirst ? remainder.peek() : first;
    }

    @Override
    public boolean hasNext() {
        return !usedFirst || remainder.hasNext();
    }

    @Override
    public boolean findHiddenArgument(String argument) {
        return remainder.findHiddenArgument(argument);
    }

    @Override
    public @Nullable String findHiddenArgumentSpecifiedValue(String argPrefix) {
        return remainder.findHiddenArgumentSpecifiedValue(argPrefix);
    }

    @Override
    public String allRemaining() {
        if (usedFirst) {
            return remainder.allRemaining();
        }
        return first + ' ' + remainder.allRemaining();
    }

    @Override
    public CommandPackage copy() {
        if (usedFirst) {
            return remainder.copy();
        }
        return new PrependedCommandPackage(first, remainder.copy());
    }

    @Override
    public String toString() {
        return "PrependedCommandPackage{" +
                "first='" + first + '\'' +
                ", remainder=" + remainder +
                ", usedFirst=" + usedFirst +
                '}';
    }
}
