/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.it.test.applicable;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.UUID;

@ExtendWith(InjectionInvocationContextProvider.class)
public class StrictStrictnessIT {

	private final StrictnessAssertHelper assertHelper;

	@Inject
	public StrictStrictnessIT(StrictnessAssertHelper helper) {
		this.assertHelper = helper;
	}

	@TestTemplate
	@SetAddressStrictness({AddressStrictness.STERN, AddressStrictness.STRICT})
	public void enforceAddressBan() {
		NetworkAddress commonAddress = RandomUtil.randomAddress();

		User userOne = User.randomUser();
		User userTwo = User.randomUser();
		User unrelatedUser = User.randomUser();
		UUID userOneAlt = UUID.randomUUID();

		// Connect users + Assume no one is banned yet
		assertHelper.connectAndAssumeUnbannedUser(userOne.uuid(), "namesdontmatter", commonAddress);
		assertHelper.connectAndAssumeUnbannedUser(userOne.uuid(), "namesdontmatter2", userOne.address());
		assertHelper.connectAndAssumeUnbannedUser(userTwo.uuid(), "yesreally", commonAddress);
		assertHelper.connectAndAssumeUnbannedUser(userTwo.uuid(), "namesdonotmatter", userTwo.address());
		assertHelper.connectAndAssumeUnbannedUser(userOneAlt, "anonymousalt", userOne.address());
		assertHelper.connectAndAssumeUnbannedUser(unrelatedUser.uuid(), "namesnevermatter", unrelatedUser.address());

		// Ban address
		assertHelper.banVictim(AddressVictim.of(userOne.address()), "Botnet is banned");

		// Same enforcement as under LENIENT
		assertHelper.assertBanned(userOne.uuid(), userOne.address(), "Address is banned");
		assertHelper.assertBanned(userOneAlt, userOne.address(), "Address is banned");
		// Same enforcement as under NORMAL
		assertHelper.assertBanned(userOne.uuid(), commonAddress, "Past address is banned");
		// The effect of STRICT
		assertHelper.assertBanned(userTwo.uuid(), commonAddress, "User is linked to banner user by present common address");
		assertHelper.assertBanned(userTwo.uuid(), userTwo.address(), "User is linked to banned user by past common address");

		// Unrelated user is not banned
		assertHelper.assertNotBanned(unrelatedUser.uuid(), unrelatedUser.address(), "Unrelated user not banned");
	}

	@TestTemplate
	@SetAddressStrictness({AddressStrictness.STERN, AddressStrictness.STRICT})
	public void enforceCompositeBan() {
		NetworkAddress commonAddress = RandomUtil.randomAddress();

		User userOne = User.randomUser();
		User userTwo = User.randomUser();
		UUID userOneAlt = UUID.randomUUID();

		// Connect users + Assume no one is banned yet
		assertHelper.connectAndAssumeUnbannedUser(userOne.uuid(), "namesdontmatter", commonAddress);
		assertHelper.connectAndAssumeUnbannedUser(userOne.uuid(), "namesdontmatter2", userOne.address());
		assertHelper.connectAndAssumeUnbannedUser(userTwo.uuid(), "yesreally", commonAddress);
		assertHelper.connectAndAssumeUnbannedUser(userTwo.uuid(), "namesdonotmatter", userTwo.address());
		assertHelper.connectAndAssumeUnbannedUser(userOneAlt, "anonymousalt", userOne.address());

		// Ban composite victim
		assertHelper.banVictim(CompositeVictim.of(userOne.uuid(), userOne.address()), "Composite user is banned");

		// Same enforcement as under LENIENT
		assertHelper.assertBanned(userOne.uuid(), userOne.address(), "UUID and address are banned");
		assertHelper.assertBanned(userOneAlt, userOne.address(), "Address is banned");
		// Same enforcement as under NORMAL
		assertHelper.assertBanned(userOne.uuid(), commonAddress, "UUID and past address are banned");
		assertHelper.assertBanned(userOneAlt, commonAddress, "Past address is banned");
		// The effect of STRICT
		assertHelper.assertBanned(userTwo.uuid(), commonAddress, "User is linked to banner user by present common address");
		assertHelper.assertBanned(userTwo.uuid(), userTwo.address(), "User is linked to banned user by past common address");
	}
}
