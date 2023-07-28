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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.config.PunishmentAdditionSection;
import space.arim.libertybans.core.config.PunishmentSection;
import space.arim.libertybans.core.env.CmdSender;

import java.time.Duration;

public record DurationPermissionCheck(CmdSender sender, DurationPermissionsConfig config, PunishmentFormatter formatter,
									  PunishmentType type, Duration attemptedDuration) implements VictimPermissionCheck {

	@Override
	public boolean checkPermission(Victim victim, PunishmentSection section) {
		if (!isDurationPermitted()) {
			// Sender does not have enough duration permissions
			String durationFormatted = formatter.formatDuration(attemptedDuration);
			PunishmentAdditionSection.WithDurationPerm sectionWithDurationPerm = ((PunishmentAdditionSection.WithDurationPerm) section);
			sender.sendMessage(sectionWithDurationPerm.permission().duration().replaceText("%DURATION%", durationFormatted));
			return false;
		}
		return true;
	}

	boolean isDurationPermitted() {
		if (type == PunishmentType.KICK) {
			return attemptedDuration.isZero();
		}
		if (!config.enable()) {
			return true;
		}
		Duration greatestPermitted = getGreatestPermittedDuration();
		if (greatestPermitted.isZero()) {
			// User can punish permanently
			return true;
		}
		if (attemptedDuration.isZero()) {
			// User cannot punish permanently but attempted duration is permanent
			return false;
		}
		// User can punish only if their permitted duration is large enough
		return greatestPermitted.compareTo(attemptedDuration) >= 0;
	}

	private Duration getGreatestPermittedDuration() {
		Duration greatestPermission = Duration.ofNanos(-1L);
		for (ParsedDuration durationPermission : config.permissionsToCheck()) {
			if (!durationPermission.hasDurationPermission(sender, type)) {
				continue;
			}
			Duration durationValue = durationPermission.duration();
			if (durationValue.isZero()) {
				// User has permission for permanent punishment
				return Duration.ZERO;
			}
			if (durationValue.compareTo(greatestPermission) > 0) {
				greatestPermission = durationValue;
			}
		}
		return greatestPermission;
	}

}
