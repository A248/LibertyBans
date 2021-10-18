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

package space.arim.libertybans.core.commands.extra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.env.CmdSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationMessageTest {

	private CommandPackage command;

	@BeforeEach
	public void setCommand() {
		command = ArrayCommandPackage.create("-s", "user");
	}

	@Test
	public void noSilentPermissions(@Mock CmdSender sender) {
		var message = new NotificationMessage(sender, PunishmentType.BAN, NotificationMessage.Mode.DO);

		when(sender.hasPermission(any())).thenReturn(false);
		message.evaluate(command);
		assertFalse(message.isSilent());
		verify(sender).hasPermission("libertybans.ban.silent");
	}

	@Test
	public void silentPermissionForDifferentType(@Mock CmdSender sender) {
		var message = new NotificationMessage(sender, PunishmentType.WARN, NotificationMessage.Mode.DO);

		lenient().when(sender.hasPermission("libertybans.mute.silent")).thenReturn(true);
		message.evaluate(command);
		assertFalse(message.isSilent());
		verify(sender).hasPermission("libertybans.warn.silent");
	}

	@Test
	public void silentPermissionForDifferentMode(@Mock CmdSender sender) {
		var message = new NotificationMessage(sender, PunishmentType.MUTE, NotificationMessage.Mode.DO);

		lenient().when(sender.hasPermission("libertybans.mute.silentundo")).thenReturn(true);
		message.evaluate(command);
		assertFalse(message.isSilent());
		verify(sender).hasPermission("libertybans.mute.silent");
	}

	@Test
	public void silentPermissionSuccess(@Mock CmdSender sender) {
		var message = new NotificationMessage(sender, PunishmentType.WARN, NotificationMessage.Mode.UNDO);

		when(sender.hasPermission("libertybans.warn.silentundo")).thenReturn(true);
		message.evaluate(command);
		assertTrue(message.isSilent());
		verify(sender).hasPermission("libertybans.warn.silentundo");
	}

	@Test
	public void notificationPermissionDo() {
		var message = new NotificationMessage(mock(CmdSender.class), PunishmentType.WARN, NotificationMessage.Mode.DO);

		assertEquals("libertybans.warn.notify", message.notificationPermission());
	}

	@Test
	public void notificationPermissionUndo() {
		var message = new NotificationMessage(mock(CmdSender.class), PunishmentType.WARN, NotificationMessage.Mode.UNDO);

		assertEquals("libertybans.warn.unnotify", message.notificationPermission());
	}

	@Test
	public void notificationPermissionDoSilent(@Mock CmdSender sender) {
		var message = new NotificationMessage(sender, PunishmentType.WARN, NotificationMessage.Mode.DO);

		when(sender.hasPermission("libertybans.warn.silent")).thenReturn(true);
		message.evaluate(command);
		assumeTrue(message.isSilent());

		assertEquals("libertybans.warn.notifysilent", message.notificationPermission());
	}

	@Test
	public void notificationPermissionUndoSilent(@Mock CmdSender sender) {
		var message = new NotificationMessage(sender, PunishmentType.WARN, NotificationMessage.Mode.UNDO);

		when(sender.hasPermission("libertybans.warn.silentundo")).thenReturn(true);
		message.evaluate(command);
		assumeTrue(message.isSilent());

		assertEquals("libertybans.warn.unnotifysilent", message.notificationPermission());
	}

}
