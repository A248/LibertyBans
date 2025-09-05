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

package space.arim.libertybans.core.env;

import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.env.annote.PlatformPlayer;

import java.util.function.Consumer;
import java.util.function.Predicate;

// The arm of the state!
public record Police<@PlatformPlayer P>(TargetMatcher targetMatcher, Predicate<@Nullable String> serverNameMatch,
                                        Consumer<P> arrest) {

    // They just have orders (field). No logic of their own (methods)
}
