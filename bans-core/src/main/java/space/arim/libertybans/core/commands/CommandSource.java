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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public interface CommandSource extends Iterator<String> {

    /**
     * Gets the current argument without advancing to the next one
     *
     * @return the current argument
     */
    String peek();

    /**
     * Creates an identical copy. Mutating one object will not affect the other.
     *
     * @return the copy
     */
    CommandSource copy();

    /**
     * Concatenates the current argument and all remaining arguments. This would
     * be equivalent to joining all calls to {@link #next()}, separating with spaces,
     * until this iterator is exhausted.
     *
     * @return the concatenated result
     */
    String allRemaining();

    final class OfArray implements CommandSource {

        private final String[] args;
        private int position;

        /**
         * Creates from an argument array. The input array is NOT cloned
         *
         * @param args the argument array, of which no elements can be null
         */
        public OfArray(String...args) {
            this.args = args;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return args[position++];
        }

        @Override
        public String peek() {
            return args[position];
        }

        @Override
        public boolean hasNext() {
            return position != args.length;
        }

        @Override
        public CommandSource copy() {
            OfArray copy = new OfArray(args);
            copy.position = position;
            return copy;
        }

        @Override
        public String allRemaining() {
            StringJoiner joiner = new StringJoiner(" ");
            for (int n = position; n < args.length; n++) {
                joiner.add(args[n]);
            }
            position = args.length;
            return joiner.toString();
        }

        @Override
        public String toString() {
            return "OfArray{" +
                    "args=" + Arrays.toString(args) +
                    ", position=" + position +
                    '}';
        }
    }

    final class OfString implements CommandSource {

        private final String args;
        private int position;

        public OfString(String args) {
            this.args = args;
        }

        private int computeNextAdvance() {
            int pos = position;
            while (pos < args.length() && args.charAt(pos) != ' ') {
                pos++;
            }
            return pos - position;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int initialPos = position;
            int nextAdvance = computeNextAdvance();
            position += nextAdvance + 1;
            return args.substring(initialPos, initialPos + nextAdvance);
        }

        @Override
        public String peek() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int initialPos = position;
            int nextAdvance = computeNextAdvance();
            return args.substring(initialPos, initialPos + nextAdvance);
        }

        @Override
        public boolean hasNext() {
            return position <= args.length();
        }

        @Override
        public String allRemaining() {
            if (!hasNext()) {
                return "";
            }
            String allRemaining = args.substring(position);
            position = args.length();
            return allRemaining;
        }

        @Override
        public CommandSource copy() {
            OfString copy = new OfString(args);
            copy.position = position;
            return copy;
        }

        @Override
        public String toString() {
            return "OfString{" +
                    "args='" + args + '\'' +
                    ", position=" + position +
                    '}';
        }
    }

    final class Prepended implements CommandSource {

        private final String first;
        private final CommandSource remainder;
        private boolean usedFirst;

        public Prepended(String first, CommandSource remainder) {
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
        public String allRemaining() {
            if (usedFirst) {
                return remainder.allRemaining();
            }
            return first + ' ' + remainder.allRemaining();
        }

        @Override
        public CommandSource copy() {
            if (usedFirst) {
                return remainder.copy();
            }
            return new Prepended(first, remainder.copy());
        }

        @Override
        public String toString() {
            return "Prepended{" +
                    "first='" + first + '\'' +
                    ", remainder=" + remainder +
                    ", usedFirst=" + usedFirst +
                    '}';
        }
    }
}
