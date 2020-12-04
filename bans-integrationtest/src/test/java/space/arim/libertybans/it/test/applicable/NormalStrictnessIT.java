/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.test.applicable;

import java.net.InetAddress;

import jakarta.inject.Inject;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import space.arim.libertybans.core.selector.AddressStrictness;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.util.RandomUtil;

@ExtendWith(InjectionInvocationContextProvider.class)
public class NormalStrictnessIT {

	private final StrictnessAssertHelper assertHelper;

	@Inject
	public NormalStrictnessIT(StrictnessAssertHelper assertHelper) {
		this.assertHelper = assertHelper;
	}

	@TestTemplate
	@SetAddressStrictness(AddressStrictness.NORMAL)
	public void testEnforceBan() {
		InetAddress commonAddress = RandomUtil.randomAddress();

		User userOne = User.randomUser();
		User userTwo = User.randomUser();
		User unrelatedUser = User.randomUser();

		// Connect users + Assume no one is banned yet
		assertHelper.connectAndAssumeUnbannedUser(userOne.uuid(), "namesdontmatter", commonAddress);
		assertHelper.connectAndAssumeUnbannedUser(userOne.uuid(), "namesdontmatter2", userOne.address());
		assertHelper.connectAndAssumeUnbannedUser(userTwo.uuid(), "yesreally", commonAddress);
		assertHelper.connectAndAssumeUnbannedUser(userTwo.uuid(), "namesdonotmatter", userTwo.address());
		assertHelper.connectAndAssumeUnbannedUser(unrelatedUser.uuid(), "namesnevermatter", unrelatedUser.address());

		// Ban address
		assertHelper.banAddress(userOne.address(), "Botnet is banned");

		// Same enforcement as under LENIENT
		assertHelper.assertBanned(userOne.uuid(), userOne.address(), "Exact address is banned"); 
		// The meaning of NORMAL
		assertHelper.assertBanned(userOne.uuid(), commonAddress, "Past address is banned");
		// Ensure no overreach
		assertHelper.assertNotBanned(userTwo.uuid(), commonAddress, "Leniency is NORMAL and not STRICT");
		assertHelper.assertNotBanned(userTwo.uuid(), userTwo.address(), "Leniency is NORMAL and not STRICT");

		// Unrelated user is not banned
		assertHelper.assertNotBanned(unrelatedUser.uuid(), unrelatedUser.address(), "Unrelated user not banned");
	}

}
