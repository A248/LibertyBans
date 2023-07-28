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

package space.arim.libertybans.core.commands.extra;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.PunishmentSection;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.Mode;
import space.arim.libertybans.core.punish.permission.PunishmentPermission;
import space.arim.libertybans.core.config.VictimPermissionSection;
import space.arim.libertybans.core.punish.permission.VictimTypeCheck;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PunishmentPermissionCheckTest {

	private final CmdSender sender;
	private final PunishmentSection section;

	private static final Component NO_PERMISSION_UUID = Component.text("You cannot do this to UUIDs");
	private static final Component NO_PERMISSION_IP = Component.text("You cannot do this to IP addresses");

	public PunishmentPermissionCheckTest(@Mock CmdSender sender, @Mock PunishmentSection section) {
		this.sender = sender;
		this.section = section;
	}

	@BeforeEach
	public void setupSection(@Mock VictimPermissionSection permissionSection) {
		lenient().when(section.permission()).thenReturn(permissionSection);
		lenient().when(permissionSection.uuid()).thenReturn(NO_PERMISSION_UUID);
		lenient().when(permissionSection.ipAddress()).thenReturn(NO_PERMISSION_IP);
	}

	private boolean checkPermission(PunishmentType type, Victim victim, Mode mode) {
		return new VictimTypeCheck(
				sender,
				new PunishmentPermission(type, mode)
		).checkPermission(victim, section);
	}

	private static PlayerVictim playerVictim() {
		return PlayerVictim.of(UUID.randomUUID());
	}

	private static AddressVictim addressVictim() {
		return AddressVictim.of(RandomUtil.randomAddress());
	}

	@ParameterizedTest
	@EnumSource
	public void noPermissionsDoUuid(PunishmentType type) {
		Victim victim = playerVictim();
		assertFalse(checkPermission(type, victim, Mode.DO));
		verify(sender).sendMessage(NO_PERMISSION_UUID);
	}

	@ParameterizedTest
	@EnumSource
	public void noPermissionsDoIP(PunishmentType type) {
		Victim victim = addressVictim();
		assertFalse(checkPermission(type, victim, Mode.DO));
		verify(sender).sendMessage(NO_PERMISSION_IP);
	}

	@ParameterizedTest
	@EnumSource
	public void noPermissionsUndoUuid(PunishmentType type) {
		Victim victim = playerVictim();
		assertFalse(checkPermission(type, victim, Mode.UNDO));
		verify(sender).sendMessage(NO_PERMISSION_UUID);
	}

	@ParameterizedTest
	@EnumSource
	public void noPermissionsUndoIP(PunishmentType type) {
		Victim victim = addressVictim();
		assertFalse(checkPermission(type, victim, Mode.UNDO));
		verify(sender).sendMessage(NO_PERMISSION_IP);
	}

	@Test
	public void canMuteUuid() {
		when(sender.hasPermission("libertybans.mute.do.target.uuid")).thenReturn(true);
		Victim victim = playerVictim();
		assertTrue(checkPermission(PunishmentType.MUTE, victim, Mode.DO));
		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void canWarnUuidButNotIPAddress() {
		lenient().when(sender.hasPermission("libertybans.warn.do.target.uuid")).thenReturn(true);
		Victim victim = addressVictim();
		assertFalse(checkPermission(PunishmentType.WARN, victim, Mode.DO));
		verify(sender).sendMessage(NO_PERMISSION_IP);
	}

	@Test
	public void canUnwarnIPAddress() {
		when(sender.hasPermission("libertybans.warn.undo.target.ip")).thenReturn(true);
		Victim victim = addressVictim();
		assertTrue(checkPermission(PunishmentType.WARN, victim, Mode.UNDO));
		verify(sender, never()).sendMessage(any());
	}
}
