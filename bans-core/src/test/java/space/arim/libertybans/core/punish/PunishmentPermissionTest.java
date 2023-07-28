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

package space.arim.libertybans.core.punish;

import org.junit.jupiter.api.Test;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.punish.permission.PunishmentPermission;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PunishmentPermissionTest {

	@Test
	public void notificationPermissionDo() {
		var message = new PunishmentPermission(PunishmentType.WARN, Mode.DO);

		assertEquals("libertybans.warn.do.notify", message.notifyPermission(false));
	}

	@Test
	public void notificationPermissionUndo() {
		var message = new PunishmentPermission(PunishmentType.WARN, Mode.UNDO);

		assertEquals("libertybans.warn.undo.notify", message.notifyPermission(false));
	}

	@Test
	public void notificationPermissionDoSilent() {
		var message = new PunishmentPermission(PunishmentType.WARN, Mode.DO);

		assertEquals("libertybans.warn.do.notifysilent", message.notifyPermission(true));
	}

	@Test
	public void notificationPermissionUndoSilent() {
		var message = new PunishmentPermission(PunishmentType.WARN, Mode.UNDO);

		assertEquals("libertybans.warn.undo.notifysilent", message.notifyPermission(true));
	}
}
