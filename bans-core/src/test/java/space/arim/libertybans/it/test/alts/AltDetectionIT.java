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

package space.arim.libertybans.it.test.alts;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.alts.AltDetection;
import space.arim.libertybans.core.alts.DetectedAlt;
import space.arim.libertybans.core.alts.DetectionKind;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetTime;
import space.arim.libertybans.it.util.RandomUtil;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static space.arim.libertybans.it.util.RandomUtil.randomName;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
public class AltDetectionIT {

	private final AltDetection altDetection;
	private final Enforcer enforcer;

	@Inject
	public AltDetectionIT(AltDetection altDetection, Enforcer enforcer) {
		this.altDetection = altDetection;
		this.enforcer = enforcer;
	}

	private static NetworkAddress randomAddress() {
		return NetworkAddress.of(RandomUtil.randomAddress());
	}

	@TestTemplate
	public void noAlts() {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress address = randomAddress();

		assumeTrue(null == enforcer.executeAndCheckConnection(uuid, name, address).join());

		assertEquals(List.of(), altDetection.detectAlts(uuid, address).join());
	}

	private static final long TIME_NOW = 1627005548L;

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void normalAlt() {
		Instant date = Instant.ofEpochSecond(TIME_NOW);
		NetworkAddress commonAddress = randomAddress();
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		UUID uuidTwo = UUID.randomUUID();
		String nameTwo = randomName();

		assumeTrue(null == enforcer.executeAndCheckConnection(uuid, name, commonAddress).join());
		assumeTrue(null == enforcer.executeAndCheckConnection(uuidTwo, nameTwo, commonAddress).join());

		assertEquals(List.of(new DetectedAlt(
				DetectionKind.NORMAL, commonAddress,
				uuidTwo, nameTwo, date)
		), altDetection.detectAlts(uuid, commonAddress).join());
		assertEquals(List.of(new DetectedAlt(
				DetectionKind.NORMAL, commonAddress,
				uuid, name, date)
		), altDetection.detectAlts(uuidTwo, commonAddress).join());
	}

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void strictAlt() {
		Instant date = Instant.ofEpochSecond(TIME_NOW);
		NetworkAddress commonPastAddress = randomAddress();
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		UUID uuidTwo = UUID.randomUUID();
		String nameTwo = randomName();

		assumeTrue(null == enforcer.executeAndCheckConnection(uuid, name, commonPastAddress).join());
		assumeTrue(null == enforcer.executeAndCheckConnection(uuidTwo, nameTwo, commonPastAddress).join());

		assertEquals(List.of(new DetectedAlt(
				DetectionKind.STRICT, commonPastAddress,
				uuidTwo, nameTwo, date)
		), altDetection.detectAlts(uuid, randomAddress()).join());
		assertEquals(List.of(new DetectedAlt(
				DetectionKind.STRICT, commonPastAddress,
				uuid, name, date)
		), altDetection.detectAlts(uuidTwo, randomAddress()).join());
	}
}
