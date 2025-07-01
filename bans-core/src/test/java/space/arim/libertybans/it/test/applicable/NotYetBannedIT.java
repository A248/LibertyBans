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

package space.arim.libertybans.it.test.applicable;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.SetAltRegistry;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.UUID;

@ExtendWith(InjectionInvocationContextProvider.class)
public class NotYetBannedIT {

	private final StrictnessAssertHelper assertHelper;

	@Inject
	public NotYetBannedIT(StrictnessAssertHelper helper) {
		this.assertHelper = helper;
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	@SetAltRegistry(all = true)
	public void testNoOneIsBanned() {
		NetworkAddress commonAddress = RandomUtil.randomAddress();

		User userOne = User.randomUser();
		User userTwo = User.randomUser();

		// Connect users + Assert no one is banned yet
		assertHelper.connectAndAssertUnbannedUser(userOne.uuid(), "namesdontmatter", commonAddress);
		assertHelper.connectAndAssertUnbannedUser(userOne.uuid(), "namesdontmatter2", userOne.address());
		assertHelper.connectAndAssertUnbannedUser(userTwo.uuid(), "yesreally", commonAddress);
		assertHelper.connectAndAssertUnbannedUser(userTwo.uuid(), "namesdonotmatter", userTwo.address());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void testOverlappingIpDigits() {
		NetworkAddress address1 = NetworkAddress.of(new byte[] {(byte) 196, 48, 80, 6});
		NetworkAddress address2 = NetworkAddress.of(new byte[] {(byte) 196, 48, 82, 6});
		NetworkAddress address3 = NetworkAddress.of(new byte[] {
				(byte) 196, 48, 80, 6, 0, 0, 1, 0, 14, 38, 20, 21, (byte) 150, 44, 12, 111});

		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		UUID user3 = UUID.randomUUID();

		// Connect users + Assert no one is banned yet
		assertHelper.connectAndAssumeUnbannedUser(user1, "p1", address1);
		assertHelper.connectAndAssumeUnbannedUser(user2, "p2", address2);
		assertHelper.connectAndAssumeUnbannedUser(user3, "p3", address3);

		// Check that the ban only affected user1
		assertHelper.banVictim(AddressVictim.of(address1), "Main ban");
		assertHelper.assertNotBanned(user2, address2, "Different IP with overlapping digits not banned");
		assertHelper.assertNotBanned(user3, address3, "Different IP with overlapping digits not banned");
	}
}
