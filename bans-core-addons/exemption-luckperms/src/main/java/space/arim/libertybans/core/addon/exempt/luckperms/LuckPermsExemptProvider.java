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

import jakarta.inject.Inject;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.addon.exempt.ExemptProvider;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class LuckPermsExemptProvider implements ExemptProvider {

	private final ExemptionLuckPermsAddon addon;
	private final FactoryOfTheFuture futuresFactory;

	@Inject
	public LuckPermsExemptProvider(ExemptionLuckPermsAddon addon, FactoryOfTheFuture futuresFactory) {
		this.addon = addon;
		this.futuresFactory = futuresFactory;
	}

	@Override
	public CompletionStage<Boolean> isExempted(CmdSender sender, String category, Victim target) {
		LuckPerms luckPerms;
		if (!addon.config().enable() || (luckPerms = addon.luckPerms()) == null) {
			return futuresFactory.completedFuture(false);
		}
		UUID senderUuid;
		if (sender.getOperator() instanceof PlayerOperator playerOperator) {
			senderUuid = playerOperator.getUUID();
		} else {
			// The console has infinite weight
			assert sender.getOperator().getType() == Operator.OperatorType.CONSOLE;
			return futuresFactory.completedFuture(false);
		}
		UUID targetUuid;
		if (target instanceof PlayerVictim playerVictim) {
			targetUuid = playerVictim.getUUID();
		} else if (target instanceof CompositeVictim compositeVictim) {
			targetUuid = compositeVictim.getUUID();
		} else {
			return futuresFactory.completedFuture(false);
		}
		UserManager userManager = luckPerms.getUserManager();
		return userManager.loadUser(senderUuid).thenCombine(userManager.loadUser(targetUuid), (senderUser, targetUser) -> {
			int senderWeight = calculateUserMaxWeight(senderUser);
			int targetWeight = calculateUserMaxWeight(targetUser);
			if (senderWeight == -1 && targetWeight == -1) return false;
			return addon.config().exemptSame() ? targetWeight >= senderWeight : targetWeight > senderWeight;
		});
	}

	private int calculateUserMaxWeight(User user) {
		int maxWeight = -1;
		for (Group group : user.getInheritedGroups(user.getQueryOptions())) {
			int weight = group.getWeight().orElse(-1);
			if (weight > maxWeight) {
				maxWeight = weight;
			}
		}
		return maxWeight;
	}

}
