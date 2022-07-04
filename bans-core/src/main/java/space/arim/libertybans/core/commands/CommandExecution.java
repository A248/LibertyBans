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

package space.arim.libertybans.core.commands;

import org.jetbrains.annotations.Nullable;
import space.arim.omnibus.util.concurrent.ReactionStage;

public interface CommandExecution {

	/**
	 * Executes a returns a future. The returned future may be null, for convenience purposes,
	 * if no asynchronous computation was initiated
	 *
	 * @return a future or {@code null} for convenience
	 */
	@Nullable ReactionStage<Void> execute();

	default void executeNow() {
		var future = execute();
		if (future != null) {
			future.toCompletableFuture().join();
		}
	}
}
