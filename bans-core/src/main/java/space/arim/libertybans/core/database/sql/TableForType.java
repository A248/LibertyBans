/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.core.database.sql;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.schema.tables.ApplicableBans;
import space.arim.libertybans.core.schema.tables.ApplicableMutes;
import space.arim.libertybans.core.schema.tables.ApplicableWarns;
import space.arim.libertybans.core.schema.tables.Bans;
import space.arim.libertybans.core.schema.tables.Mutes;
import space.arim.libertybans.core.schema.tables.SimpleBans;
import space.arim.libertybans.core.schema.tables.SimpleMutes;
import space.arim.libertybans.core.schema.tables.SimpleWarns;
import space.arim.libertybans.core.schema.tables.Warns;

import java.util.Objects;

public final class TableForType {

	private final PunishmentType type;

	public TableForType(PunishmentType type) {
		this.type = Objects.requireNonNull(type, "type");
	}

	public RawPunishmentFields dataTable() {
		return switch (type) {
			case BAN -> new RawBansFields(Bans.BANS);
			case MUTE -> new RawMutesFields(Mutes.MUTES);
			case WARN -> new RawWarnsFields(Warns.WARNS);
			case KICK -> throw new UnsupportedOperationException("Does not exist for kicks");
		};
	}

	public SimpleViewFields simpleView() {
		return switch (type) {
			case BAN -> new SimpleBansFields(SimpleBans.SIMPLE_BANS);
			case MUTE -> new SimpleMutesFields(SimpleMutes.SIMPLE_MUTES);
			case WARN -> new SimpleWarnsFields(SimpleWarns.SIMPLE_WARNS);
			case KICK -> throw new UnsupportedOperationException("Does not exist for kicks");
		};
	}

	public ApplicableViewFields applicableView() {
		return switch (type) {
			case BAN -> new ApplicableBansFields(ApplicableBans.APPLICABLE_BANS);
			case MUTE -> new ApplicableMutesFields(ApplicableMutes.APPLICABLE_MUTES);
			case WARN -> new ApplicableWarnsFields(ApplicableWarns.APPLICABLE_WARNS);
			case KICK -> throw new UnsupportedOperationException("Does not exist for kicks");
		};
	}

	@Override
	public String toString() {
		return "TableForType{" +
				"type=" + type +
				'}';
	}
}
