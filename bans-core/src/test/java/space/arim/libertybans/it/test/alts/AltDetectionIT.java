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
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.alts.AltDetection;
import space.arim.libertybans.core.alts.DetectedAlt;
import space.arim.libertybans.core.alts.DetectionKind;
import space.arim.libertybans.core.alts.WhichAlts;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.libertybans.core.punish.Guardian;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetTime;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static space.arim.libertybans.core.alts.WhichAlts.ALL_ALTS;
import static space.arim.libertybans.core.alts.WhichAlts.BANNED_ALTS;
import static space.arim.libertybans.core.alts.WhichAlts.BANNED_OR_MUTED_ALTS;
import static space.arim.libertybans.it.util.RandomUtil.randomAddress;
import static space.arim.libertybans.it.util.RandomUtil.randomName;

@ExtendWith(InjectionInvocationContextProvider.class)
public class AltDetectionIT {

	private final AltDetection altDetection;
	private final Guardian guardian;
	private final PunishmentDrafter drafter;

	@Inject
	public AltDetectionIT(AltDetection altDetection, Guardian guardian, PunishmentDrafter drafter) {
		this.altDetection = altDetection;
		this.guardian = guardian;
		this.drafter = drafter;
	}

	private void testNoAlts(WhichAlts whichAlts) {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress address = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, address).join());
		assertEquals(List.of(), altDetection.detectAlts(uuid, address, whichAlts).join());
	}

	@TestTemplate
	public void noAlts() {
		for (WhichAlts whichAlts : WhichAlts.values()) {
			assertDoesNotThrow(() -> testNoAlts(whichAlts), () -> "Using WhichAlts " + whichAlts);
		}
	}

	private static final long TIME_NOW = 1627005548L;
	private static final Instant DATE_NOW = Instant.ofEpochSecond(TIME_NOW);

	private void testNormalAlt(WhichAlts whichAltsForFirstAltCheck,
							   PunishmentType expectedPunishmentTypeForFirstAltCheck,
							   Consumer<UUID> operationOnAltBeforeAltCheck) {
		NetworkAddress commonAddress = randomAddress();
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		UUID uuidTwo = UUID.randomUUID();
		String nameTwo = randomName();

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, commonAddress).join());
		assumeTrue(null == guardian.executeAndCheckConnection(uuidTwo, nameTwo, commonAddress).join());

		operationOnAltBeforeAltCheck.accept(uuidTwo);

		assertEquals(List.of(new DetectedAlt(
				DetectionKind.NORMAL, expectedPunishmentTypeForFirstAltCheck, commonAddress,
				uuidTwo, nameTwo, DATE_NOW)
		), altDetection.detectAlts(uuid, commonAddress, whichAltsForFirstAltCheck).join());
		assertEquals(List.of(new DetectedAlt(
				DetectionKind.NORMAL, null, commonAddress,
				uuid, name, DATE_NOW)
		), altDetection.detectAlts(uuidTwo, commonAddress, ALL_ALTS).join());
	}

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void normalAlt() {
		testNormalAlt(ALL_ALTS, null, (uuid) -> {});
	}

	private void addPunishment(UUID uuid, PunishmentType type) {
		drafter.draftBuilder()
				.type(type)
				.victim(PlayerVictim.of(uuid))
				.operator(ConsoleOperator.INSTANCE)
				.reason("reason")
				.build()
				.enactPunishment(EnforcementOpts.builder().enforcement(EnforcementOptions.Enforcement.NONE).build())
				.toCompletableFuture().join();
	}

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void normalBannedAlt() {
		Consumer<UUID> punishment = (uuid) -> {
			addPunishment(uuid, PunishmentType.BAN);
		};
		testNormalAlt(ALL_ALTS, PunishmentType.BAN, punishment);
		testNormalAlt(BANNED_OR_MUTED_ALTS, PunishmentType.BAN, punishment);
		testNormalAlt(BANNED_ALTS, PunishmentType.BAN, punishment);
	}

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void normalMutedAlt() {
		Consumer<UUID> punishment = (uuid) -> {
			addPunishment(uuid, PunishmentType.MUTE);
		};
		testNormalAlt(ALL_ALTS, PunishmentType.MUTE, punishment);
		testNormalAlt(BANNED_OR_MUTED_ALTS, PunishmentType.MUTE, punishment);
	}

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void normalBannedAndMutedAlt() {
		Consumer<UUID> punishment = (UUID uuid) -> {
			addPunishment(uuid, PunishmentType.BAN);
			addPunishment(uuid, PunishmentType.MUTE);
		};
		// When both banned and muted, ban should take precedence
		PunishmentType expectedPunishment = PunishmentType.BAN;
		testNormalAlt(ALL_ALTS, expectedPunishment, punishment);
		testNormalAlt(BANNED_OR_MUTED_ALTS, expectedPunishment, punishment);
		testNormalAlt(BANNED_ALTS, expectedPunishment, punishment);
	}

	@TestTemplate
	@SetTime(unixTime = TIME_NOW)
	public void strictAlt(SettableTime time) {
		NetworkAddress commonPastAddress = randomAddress();
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress newAddress = randomAddress();
		UUID uuidTwo = UUID.randomUUID();
		String nameTwo = randomName();
		NetworkAddress newAddressTwo = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, commonPastAddress).join());
		assumeTrue(null == guardian.executeAndCheckConnection(uuidTwo, nameTwo, commonPastAddress).join());
		// Detects a bug, now solved, involving reliance on the "updated" database column
		time.advanceBy(Duration.ofDays(1L));
		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, newAddress).join());
		assumeTrue(null == guardian.executeAndCheckConnection(uuidTwo, nameTwo, newAddressTwo).join());

		assertEquals(List.of(new DetectedAlt(
				DetectionKind.STRICT, null, commonPastAddress,
				uuidTwo, nameTwo, DATE_NOW)
		), altDetection.detectAlts(uuid, newAddress, ALL_ALTS).join());
		assertEquals(List.of(new DetectedAlt(
				DetectionKind.STRICT, null, commonPastAddress,
				uuid, name, DATE_NOW)
		), altDetection.detectAlts(uuidTwo, newAddressTwo, ALL_ALTS).join());
	}
}
