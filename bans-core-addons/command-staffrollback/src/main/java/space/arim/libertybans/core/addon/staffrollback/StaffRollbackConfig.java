/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.addon.staffrollback;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.addon.AddonConfig;

public interface StaffRollbackConfig extends AddonConfig {

	@ConfDefault.DefaultString("&cUsage: /libertybans staffrollback <operator> [time]")
	Component usage();

	@ConfKey("invalid-duration")
	@ConfDefault.DefaultString("&cInvalid duration specified.")
	Component invalidDuration();

	@ConfKey("no-permission")
	@ConfDefault.DefaultString("&cSorry, you cannot use this.")
	Component noPermission();

	@ConfKey("no-punishments-to-rollback")
	@ConfDefault.DefaultString("&e%OPERATOR%&7 has not punished anyone in the specified time period.")
	ComponentText noPunishmentsToRollback();

	@SubSection
	Confirmation confirmation();

	@ConfHeader({
			"If enabled, users will be sent a confirmation message asking if they wish to proceed with the rollback",
			"The confirmation feature uses a confirmation code which is intended to be used with clickable text."
	})
	interface Confirmation {

		@ConfComments("Whether to enable this feature")
		@ConfDefault.DefaultBoolean(true)
		boolean enable();

		@ConfDefault.DefaultStrings({
				"&7You are about to purge all punishments by &e%OPERATOR%&7, numbering &e%COUNT%&7 in total.",
				"&c&lThis cannot be undone. ||&7&aClick here to confirm.||ttp:&7Click here||cmd:/libertybans staffrollback confirm:%CONFIRMATION_CODE%"
		})
		ComponentText message();

		@ConfKey("invalid-code")
		@ConfDefault.DefaultString("&cInvalid confirmation code.")
		Component invalidCode();

		@ConfKey("nonexistent-code")
		@ConfDefault.DefaultString("&cThat confirmation has either expired or it does not exist.")
		Component nonexistentCode();

		@ConfKey("expiration-time-seconds")
		@ConfComments("The amount of time, in seconds, before the confirmation code expires.")
		@ConfDefault.DefaultInteger(60)
		int expirationTimeSeconds();

	}

	@ConfDefault.DefaultString("&7Rolled back &e%COUNT%&7 punishments by &e%OPERATOR%&7.")
	ComponentText success();

}
