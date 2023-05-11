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

package space.arim.libertybans.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.SubSection;

public interface PunishmentAdditionSection extends PunishmentSection {

	ComponentText SHOULD_NOT_CONFLICT = ComponentText.create(Component.text(
			"Unable to add punishment due to a conflict, but a conflict is impossible. Please report this as a bug",
			NamedTextColor.RED
	));

	Component usage();

	@SubSection
	@Override
	VictimPermissionSection permission();

	ComponentText exempted();

	ComponentText conflicting();

	ComponentText successMessage();

	ComponentText successNotification();

	interface WithLayout extends PunishmentAdditionSection {

		ComponentText layout();

	}

	interface WithDurationPerm extends WithLayout {

		@Override
		@SubSection
		VictimPermissionSection.WithDuration permission();

	}

}
