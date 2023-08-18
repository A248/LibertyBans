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

package space.arim.libertybans.core.env;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

/**
 * Base class for listeners which initiate a computation at a low priority and
 * complete at a higher priority.
 * 
 * @author A248
 *
 * @param <E> the event type
 * @param <R> the computation result type
 */
public abstract class ParallelisedListener<E, R> implements PlatformListener {

	private final Map<E, CentralisedFuture<R>> events = Collections.synchronizedMap(new IdentityHashMap<>());

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	protected ParallelisedListener() {
	}

	/**
	 * Initiates a computation for an event
	 * 
	 * @param event       the event
	 * @param computation the future
	 */
	protected final void begin(E event, CentralisedFuture<R> computation) {
		Objects.requireNonNull(computation, "computation");
		CentralisedFuture<R> previous = events.put(event, computation);
		if (previous != null) {
			logger.warn("Replaced existing computation for {}; previous result is {}", event, computation.join());
		}
	}

	protected final void debugPrematurelyDenied(E event) {
		logger.trace("Event {} is already blocked", event);
	}

	protected final void absentFutureHandler(E event) {
		if (isAllowed(event)) {
			logger.error("You likely have a misbehaving plugin installed on your server. " +
					"\nThis may lead to bans or mutes not being checked and enforced." +
					"\n\n" +
					"Reason: The event {} was previously blocked by the server or another plugin, "
					+ "but since then, some plugin has *uncancelled* the blocking.", event);
		} else {
			logger.trace("Event {} is already blocked (confirmation)", event);
		}
	}

	protected abstract boolean isAllowed(E event);

	protected final void debugResultPermitted(E event) {
		logger.trace("Event {} will be permitted", event);
	}

	/**
	 * Withdraws the computation
	 * 
	 * @param event the event
	 * @return the future, or null if no computation was started
	 */
	protected CentralisedFuture<R> withdrawRaw(E event) {
		return events.remove(event);
	}

	/**
	 * Withdraws the result, waiting if necessary if it has not yet completed. <br>
	 * If no computation is present, this returns {@code null}. Else it returns the
	 * result of the future.
	 * 
	 * @param event the event
	 * @return the future
	 */
	protected R withdraw(E event) {
		CentralisedFuture<R> future = events.remove(event);
		if (future == null) {
			absentFutureHandler(event);
			return null;
		}
		return future.join();
	}

}
