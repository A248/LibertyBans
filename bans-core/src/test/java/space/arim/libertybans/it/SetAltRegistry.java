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

package space.arim.libertybans.it;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface SetAltRegistry {

    /**
     * The settings to test against.
     *
     * @return the alt registry options
     */
    Option[] value() default {};

    /**
     * Whether to test against all alt registry options. Overrides {@code value}
     *
     * @return true to use all options
     */
    boolean all() default false;

    enum Option {
        /**
         * Registers on immediate connection. The default behavior.
         */
        ON_CONNECTION,
        /**
         * Registers the player account when they switch to another server, except {@link #NON_REGISTERING_SERVER_NAME}
         */
        ON_SERVER_SWITCH;

        public static final String NON_REGISTERING_SERVER_NAME = "no_register_alts";
    }
}
