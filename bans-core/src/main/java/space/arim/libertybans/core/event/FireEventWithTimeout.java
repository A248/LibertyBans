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

package space.arim.libertybans.core.event;

import jakarta.inject.Inject;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.events.AsyncEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public record FireEventWithTimeout(Omnibus omnibus) {

	@Inject
	public FireEventWithTimeout {}

	public <E extends AsyncEvent> CompletableFuture<E> fire(E event) {
		return omnibus.getEventBus().fireAsyncEvent(event).orTimeout(10L, TimeUnit.SECONDS);
	}

}
