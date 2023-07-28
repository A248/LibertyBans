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

import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.core.env.CmdSender;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static space.arim.libertybans.core.addon.exempt.vault.ExemptionVaultConfig.PermissionCheckThreadContext.REQUIRE_ASYNC;
import static space.arim.libertybans.core.addon.exempt.vault.ExemptionVaultConfig.PermissionCheckThreadContext.REQUIRE_MAIN_THREAD;
import static space.arim.libertybans.core.addon.exempt.vault.ExemptionVaultConfig.PermissionCheckThreadContext.RUN_ANYWHERE;
import static space.arim.libertybans.core.addon.exempt.vault.ExemptionVaultConfig.PermissionCheckThreadContext.USE_ASYNC_FOR_OFFLINE_PLAYERS;

@ExtendWith(MockitoExtension.class)
public class VaultExemptProviderTest {

	private final ExemptionVaultAddon addon;
	private final ControllableFactoryOfTheFuture futuresFactory = new ControllableFactoryOfTheFuture();
	private final Server server;

	private CmdSender sender;
	private UUID targetUuid;
	private Permission permissions;
	private VaultExemptProvider exemptProvider;

	public VaultExemptProviderTest(@Mock ExemptionVaultAddon addon, @Mock Server server) {
		this.addon = addon;
		this.server = server;
	}

	@BeforeEach
	public void setup(@Mock CmdSender sender, @Mock Permission permissions) {
		this.sender = sender;
		targetUuid = UUID.randomUUID();
		this.permissions = permissions;
		exemptProvider = new VaultExemptProvider(addon, futuresFactory, server);
	}

	private void setConfig(int maxLevelToScanFor, ExemptionVaultConfig.PermissionCheckThreadContext threadContext) {
		ExemptionVaultConfig config = mock(ExemptionVaultConfig.class);
		when(config.enable()).thenReturn(true);
		when(config.maxLevelToScanFor()).thenReturn(maxLevelToScanFor);
		when(config.permissionCheckThreadContext()).thenReturn(threadContext);
		when(addon.config()).thenReturn(config);
		when(addon.permissions()).thenReturn(permissions);
	}

	private CompletableFuture<Boolean> checkExempted() {
		return exemptProvider.isExempted(
				sender, "ban", PlayerVictim.of(targetUuid)
		).toCompletableFuture();
	}

	private void assertIsExempted(boolean exempt) {
		assertEquals(exempt, checkExempted().join());
	}

	@Test
	public void noPermissionsConfigured() {
		setConfig(50, RUN_ANYWHERE);
		when(sender.hasPermission(any())).thenReturn(true);
		when(sender.hasPermission(startsWith("libertybans.ban.exempt.level."))).thenReturn(false);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(mock(OfflinePlayer.class));
		assertIsExempted(false);
	}

	@Test
	public void hasExemptPermission(@Mock OfflinePlayer targetPlayer) {
		setConfig(50, RUN_ANYWHERE);
		when(sender.hasPermission(any())).thenReturn(true);
		when(sender.hasPermission(startsWith("libertybans.ban.exempt.level."))).thenReturn(false);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		lenient().when(permissions.playerHas(null, targetPlayer, "libertybans.ban.exempt.level.10")).thenReturn(true);
		assertIsExempted(true);
	}

	@Test
	public void hasExemptPermissionBeyondLevelToScanFor(@Mock OfflinePlayer targetPlayer) {
		setConfig(1, RUN_ANYWHERE);
		when(sender.hasPermission(any())).thenReturn(true);
		when(sender.hasPermission(startsWith("libertybans.ban.exempt.level."))).thenReturn(false);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		lenient().when(permissions.playerHas(null, targetPlayer, "libertybans.ban.exempt.level.2")).thenReturn(true);
		assertIsExempted(false);
	}

	@Test
	public void targetHasExemptLevelHigherThanOperatorLevel(@Mock OfflinePlayer targetPlayer) {
		setConfig(50, RUN_ANYWHERE);
		when(sender.hasPermission(any())).thenReturn(true);
		when(sender.hasPermission(startsWith("libertybans.ban.exempt.level."))).thenReturn(false);
		when(sender.hasPermission("libertybans.ban.exempt.level.20")).thenReturn(true);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		lenient().when(permissions.playerHas(null, targetPlayer, "libertybans.ban.exempt.level.30")).thenReturn(true);
		assertIsExempted(true);
	}

	@Test
	public void targetHasExemptLevelLowerThanOperatorLevel(@Mock OfflinePlayer targetPlayer) {
		setConfig(50, RUN_ANYWHERE);
		when(sender.hasPermission(any())).thenReturn(true);
		when(sender.hasPermission(startsWith("libertybans.ban.exempt.level."))).thenReturn(false);
		when(sender.hasPermission("libertybans.ban.exempt.level.20")).thenReturn(true);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		lenient().when(permissions.playerHas(null, targetPlayer, "libertybans.ban.exempt.level.10")).thenReturn(true);
		assertIsExempted(false);
	}

	@Test
	public void requireMainThread(@Mock OfflinePlayer targetPlayer) {
		setConfig(4, REQUIRE_MAIN_THREAD);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		var future = checkExempted();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runAsyncTasks();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runSyncTasks();
		assertTrue(future.isDone());
		verify(permissions, times(5)).playerHas(isNull(), eq(targetPlayer), any());
	}

	@Test
	public void requireAsync(@Mock OfflinePlayer targetPlayer) {
		setConfig(4, REQUIRE_ASYNC);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		var future = checkExempted();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runSyncTasks();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runAsyncTasks();
		assertTrue(future.isDone(), "" + futuresFactory);
		verify(permissions, times(5)).playerHas(isNull(), eq(targetPlayer), any());
	}

	@Test
	public void useAsyncForOfflinePlayersTargetOnline(@Mock Player targetPlayer) {
		setConfig(4, USE_ASYNC_FOR_OFFLINE_PLAYERS);
		when(server.getPlayer(targetUuid)).thenReturn(targetPlayer);
		var future = checkExempted();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runSyncTasks();
		assertTrue(future.isDone());
		verify(permissions, times(5)).playerHas(isNull(), eq(targetPlayer), any());
	}

	@Test
	public void useAsyncForOfflinePlayersTargetOffline(@Mock OfflinePlayer targetPlayer) {
		setConfig(4, USE_ASYNC_FOR_OFFLINE_PLAYERS);
		when(server.getPlayer(targetUuid)).thenReturn(null);
		when(server.getOfflinePlayer(targetUuid)).thenReturn(targetPlayer);
		var future = checkExempted();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runSyncTasks();
		assertFalse(future.isDone());
		verifyNoInteractions(permissions);
		futuresFactory.runAsyncTasks();
		assertTrue(future.isDone());
		verify(permissions, times(5)).playerHas(isNull(), eq(targetPlayer), any());
	}

}
