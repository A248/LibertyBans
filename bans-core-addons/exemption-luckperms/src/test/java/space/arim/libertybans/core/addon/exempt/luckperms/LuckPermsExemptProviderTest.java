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

package space.arim.libertybans.core.addon.exempt.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LuckPermsExemptProviderTest {

	private final ExemptionLuckPermsAddon addon;

	private UUID senderUuid;
	private UUID targetUuid;
	private User senderUser;
	private User targetUser;
	private ExemptionLuckPermsConfig config;
	private LuckPermsExemptProvider exemptProvider;

	public LuckPermsExemptProviderTest(@Mock ExemptionLuckPermsAddon addon) {
		this.addon = addon;
	}

	@BeforeEach
	public void setup(@Mock ExemptionLuckPermsConfig config, @Mock LuckPerms luckPerms,
					  @Mock UserManager userManager, @Mock User senderUser, @Mock User targetUser) {
		FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
		senderUuid = UUID.randomUUID();
		targetUuid = UUID.randomUUID();
		this.senderUser = senderUser;
		this.targetUser = targetUser;
		when(config.enable()).thenReturn(true);
		this.config = config;
		when(addon.config()).thenReturn(config);
		when(addon.luckPerms()).thenReturn(luckPerms);
		when(luckPerms.getUserManager()).thenReturn(userManager);
		when(userManager.loadUser(senderUuid)).thenReturn(futuresFactory.completedFuture(senderUser));
		when(userManager.loadUser(targetUuid)).thenReturn(futuresFactory.completedFuture(targetUser));
		exemptProvider = new LuckPermsExemptProvider(addon, futuresFactory);
	}

	private void setGroups(User user, Integer...groupWeights) {
		Set<Group> groups = new HashSet<>();
		for (Integer groupWeight : groupWeights) {
			Group group = mock(Group.class);
			when(group.getWeight()).thenReturn(groupWeight == null ? OptionalInt.empty() : OptionalInt.of(groupWeight));
			groups.add(group);
		}
		QueryOptions queryOptions = mock(QueryOptions.class);
		when(user.getQueryOptions()).thenReturn(queryOptions);
		when(user.getInheritedGroups(queryOptions)).thenReturn(groups);
	}

	private void assertIsExempted(boolean outcome) {
		assertEquals(outcome, exemptProvider.isExempted(
				when(mock(CmdSender.class).getOperator()).thenReturn(PlayerOperator.of(senderUuid)).getMock(),
				"ban",
				PlayerVictim.of(targetUuid)
		).toCompletableFuture().join());
	}

	@Test
	public void noWeightsConfigured() {
		setGroups(senderUser, null, null, null);
		setGroups(targetUser, null, null);
		assertIsExempted(false);
	}

	@Test
	public void hasExemptWeight() {
		setGroups(senderUser, null, null);
		setGroups(targetUser, null, 10, null);
		assertIsExempted(true);
	}

	@Test
	public void targetHasExemptWeightHigherThanOperatorWeight() {
		setGroups(senderUser, null, null, 4, null, null, 5);
		setGroups(targetUser, 3, null, 10, null, 0);
		assertIsExempted(true);
	}

	@Test
	public void targetHasExemptWeightLowerThanOperatorWeight() {
		setGroups(senderUser, null, null, 4, null, null, 5, 20);
		setGroups(targetUser, 3, null, 10, null, 0);
		assertIsExempted(false);
	}

	@Test
	public void targetHasExemptWeightSameAsOperatorWeight() {
		setGroups(senderUser, null, null, 4, null, null, 5, 20);
		setGroups(targetUser, 3, null, 10, null, 20, 9);
		assertIsExempted(false);
	}

	@Test
	public void targetHasExemptWeightSameAsOperatorWeightOptionEnabled() {
		setGroups(senderUser, null, null, 4, null, null, 5, 20);
		setGroups(targetUser, 3, null, 10, null, 20, 9);
		when(config.exemptSame()).thenReturn(true);
		assertIsExempted(true);
	}

}
