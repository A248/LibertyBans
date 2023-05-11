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

package space.arim.libertybans.core.addon.exempt.vault;

import jakarta.inject.Inject;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.addon.exempt.ExemptProvider;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class VaultExemptProvider implements ExemptProvider {

	private final ExemptionVaultAddon addon;
	private final FactoryOfTheFuture futuresFactory;
	private final Server server;

	@Inject
	public VaultExemptProvider(ExemptionVaultAddon addon, FactoryOfTheFuture futuresFactory, Server server) {
		this.addon = addon;
		this.futuresFactory = futuresFactory;
		this.server = server;
	}

	@Override
	public CompletionStage<Boolean> isExempted(CmdSender sender, String category, Victim target) {
		ExemptionVaultConfig config;
		Permission permissions;
		if (!(config = addon.config()).enable() || (permissions = addon.permissions()) == null) {
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
		ExemptionCheck check = new ExemptionCheck(sender, category, targetUuid, permissions, config.maxLevelToScanFor());
		return switch (config.permissionCheckThreadContext()) {

			case RUN_ANYWHERE -> futuresFactory.completedFuture(check.check());
			case REQUIRE_MAIN_THREAD -> futuresFactory.supplySync(check::check);
			case REQUIRE_ASYNC -> futuresFactory.supplyAsync(check::check);

			case USE_ASYNC_FOR_OFFLINE_PLAYERS -> futuresFactory.supplySync(() -> {
				Player player = server.getPlayer(targetUuid);
				if (player == null) {
					return futuresFactory.supplyAsync(check::check);
				} else {
					return futuresFactory.completedFuture(check.checkWith(player));
				}
			}).thenCompose(future -> future);
		};
	}

	private final class ExemptionCheck {

		private final CmdSender sender;
		private final String category;
		private final UUID targetUuid;
		private final Permission permissions;
		private final int maxLevelToScanFor;

		private ExemptionCheck(CmdSender sender, String category, UUID targetUuid,
							   Permission permissions, int maxLevelToScanFor) {
			this.sender = sender;
			this.category = category;
			this.targetUuid = targetUuid;
			this.permissions = permissions;
			this.maxLevelToScanFor = maxLevelToScanFor;
		}

		private boolean check() {
			return checkWith(server.getOfflinePlayer(targetUuid));
		}

		private boolean checkWith(OfflinePlayer targetPlayer) {
			String permissionPrefix = "libertybans." + category + ".exempt.level.";
			// Determine operator's level first; scan descending
			int operatorLevel = -1;
			for (int n = maxLevelToScanFor; n >= 0; n--) {
				if (sender.hasPermission(permissionPrefix + n)) {
					operatorLevel = n;
					break;
				}
			}
			// Determine whether target's level exceeds operator level
			for (int n = maxLevelToScanFor; n > operatorLevel; n--) {
				if (permissions.playerHas(null, targetPlayer, permissionPrefix + n)) {
					return true;
				}
			}
			return false;
		}

	}

}
