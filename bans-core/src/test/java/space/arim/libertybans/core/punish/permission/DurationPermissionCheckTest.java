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

package space.arim.libertybans.core.punish.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.env.CmdSender;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DurationPermissionCheckTest {

	private final CmdSender sender;
	private final DurationPermissionsConfig config;

	public DurationPermissionCheckTest(@Mock CmdSender sender, @Mock DurationPermissionsConfig config) {
		this.sender = sender;
		this.config = config;
	}

	private boolean isBanPermitted(Duration duration) {
		return new DurationPermissionCheck(
				sender, config, mock(InternalFormatter.class), PunishmentType.BAN, duration
		).isDurationPermitted();
	}

	@BeforeEach
	public void registerPermissions() {
		when(config.enable()).thenReturn(true);
		when(config.permissionsToCheck()).thenReturn(Set.of(
				new ParsedDuration("1m", Duration.ofMinutes(1L)),
				new ParsedDuration("4h", Duration.ofHours(4L)),
				new ParsedDuration("perm", Duration.ZERO))
		);
	}

	@Test
	public void isDurationPermitted() {
		assertFalse(isBanPermitted(Duration.ofHours(3L)), "User has no permissions");

		when(sender.hasPermission("libertybans.ban.dur.1m")).thenReturn(true);
		assertFalse(isBanPermitted(Duration.ofHours(3L)), "User does not have sufficient permission");
		assertFalse(isBanPermitted(Duration.ZERO), "User cannot ban permanently");

		when(sender.hasPermission("libertybans.ban.dur.4h")).thenReturn(true);
		assertTrue(isBanPermitted(Duration.ofHours(3L)), "User has permission for 4h which is greater than 3h");
		assertFalse(isBanPermitted(Duration.ofDays(1L)), "User does not have sufficient permission for 1 day");

		when(sender.hasPermission("libertybans.ban.dur.perm")).thenReturn(true);
		assertTrue(isBanPermitted(Duration.ofDays(30L)), "User can ban permanently so 30 days is acceptable");
		assertTrue(isBanPermitted(Duration.ZERO), "User can ban permanently");
	}

}
