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

package space.arim.libertybans.it.test.uuid;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static space.arim.libertybans.it.util.RandomUtil.randomAddress;
import static space.arim.libertybans.it.util.RandomUtil.randomName;

@ExtendWith(InjectionInvocationContextProvider.class)
public class UUIDStoreIT {

	private final UUIDManager uuidManager;

	public UUIDStoreIT(UUIDManager uuidManager) {
		this.uuidManager = uuidManager;
	}

	private String lookupName(UUID uuid) {
		return uuidManager.lookupName(uuid).join().orElse(null);
	}

	private UUID lookupUUID(String name) {
		return uuidManager.lookupUUID(name).join().orElse(null);
	}

	private NetworkAddress lookupAddress(String name) {
		return uuidManager.lookupAddress(name).join();
	}

	private UUIDAndAddress lookupPlayer(String name) {
		return uuidManager.lookupPlayer(name).join().orElse(null);
	}

	@TestTemplate
	public void noStoredUuid() {
		String name = randomName();
		assertNull(lookupUUID(name));
		assertNull(lookupAddress(name));
		assertNull(lookupPlayer(name));
	}

	@TestTemplate
	public void noStoredName(){
		assertNull(lookupName(UUID.randomUUID()));
	}

	@TestTemplate
	public void storeJoiningUuid(Guardian guardian, InternalDatabase database) {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress address = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, address).join());

		assertEquals(name, lookupName(uuid));
		assertEquals(uuid, lookupUUID(name));
		assertEquals(address, lookupAddress(name), "Failed for vendor " + database.getVendor());
	}

	@TestTemplate
	public void useLatestName(Guardian guardian, SettableTime time) {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress address = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, address).join());

		assumeTrue(name.equals(lookupName(uuid)));
		assumeTrue(uuid.equals(lookupUUID(name)));
		assumeTrue(address.equals(lookupAddress(name)));

		String recentName = randomName();
		NetworkAddress recentAddress = randomAddress();

		// Advance to make the previous name and address outdated
		time.advanceBy(Duration.ofSeconds(2L));

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, recentName, recentAddress).join());

		assertEquals(recentName, lookupName(uuid), "Should use most recent name");
		assertEquals(recentAddress, lookupAddress(recentName), "Should use most recent address with most recent name");
		assertEquals(recentAddress, lookupAddress(name), "Should use most recent address with past name");

		assertEquals(uuid, lookupUUID(recentName));
		assertEquals(uuid, lookupUUID(name), "Should still be able to look up by past name");
	}

	@TestTemplate
	public void useLatestAddressInLookupPlayer(Guardian guardian, SettableTime time) {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress addressOne = randomAddress();
		NetworkAddress addressTwo = randomAddress();

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, addressOne).join());

		time.advanceBy(Duration.ofHours(1L));

		assumeTrue(null == guardian.executeAndCheckConnection(uuid, name, addressTwo).join());

		time.advanceBy(Duration.ofMinutes(5L));

		assertEquals(addressTwo, lookupPlayer(name).address(), "Should use most recent address");
	}
}
