/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.core.uuid.ServerType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Details related to the platform implementation
 *
 */
// Thankfully, annotations implement sane equals and hashCode
@Retention(RUNTIME)
@Target(METHOD)
public @interface PlatformSpecs {

	ServerTypes serverTypes() default @ServerTypes(value = {}, all = true);

	// Make sure these defaults match those in ConfigSpecPossibilities

	InstanceType instanceType() default InstanceType.PROXY;

	boolean pluginMessaging() default false;

	@interface ServerTypes {

		/**
		 * Sets the server types
		 *
		 * @return the server types to test
		 */
		ServerType[] value();

		/**
		 * Whether to use all server types. Overrides  the value
		 *
		 * @return true to use all types
		 */
		boolean all() default false;

	}
}
