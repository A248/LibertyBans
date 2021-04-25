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

package space.arim.libertybans.core.env;

import org.junit.jupiter.api.Test;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ParallelisedListenerTest {

	private final SampleListener listener = new SampleListener();
	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	private static int nonZeroInteger() {
		int value;
		do {
			value = ThreadLocalRandom.current().nextInt();
		} while (value == 0);
		return value;
	}

	private static class SampleListener extends ParallelisedListener<SampleEvent, SampleResult> {

		@Override
		protected boolean isAllowed(SampleEvent event) {
			return event.value() != 0;
		}

		@Override
		public void register() { }

		@Override
		public void unregister() { }

	}

	@Test
	public void addAndWithdraw() {
		int value = nonZeroInteger();
		var evt = new SampleEvent(value);
		var result = new SampleResult(value);
		listener.begin(evt, futuresFactory.completedFuture(result));

		SampleResult withdrawn = listener.withdraw(evt);
		assertEquals(result, withdrawn);
		assertSame(result, withdrawn);
	}

	@Test
	public void identityEqualityOfEvents() {
		int evtValue = nonZeroInteger();
		var evt1 = new SampleEvent(evtValue);
		var evt2 = new SampleEvent(evtValue);

		listener.begin(evt1, futuresFactory.completedFuture(new SampleResult(1)));
		listener.begin(evt2, futuresFactory.completedFuture(new SampleResult(2)));

		assertEquals(listener.withdraw(evt1), new SampleResult(1));
		assertEquals(listener.withdraw(evt2), new SampleResult(2));
	}

	@Test
	public void absentFutureHandler() {
		int value = 0;
		var evt = new SampleEvent(value);

		assertNull(listener.withdrawRaw(evt));
		assertNull(listener.withdraw(evt));
	}

	@Test
	public void absentFutureHandlerUnallowedEvent() {
		int value = nonZeroInteger();
		var evt = new SampleEvent(value);

		assertNull(listener.withdrawRaw(evt));
		assertNull(listener.withdraw(evt));
	}

	@Test
	public void absentFutureHandlerIdentityEquality() {
		int value = 0;
		var evt1 = new SampleEvent(value);
		var evt2 = new SampleEvent(value);

		listener.begin(evt2, futuresFactory.completedFuture(new SampleResult(value)));
		assertNull(listener.withdrawRaw(evt1));
		assertEquals(listener.withdraw(evt2), new SampleResult(value));
	}

}
