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

package space.arim.libertybans.core.database.sql;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Victim;

import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.Victims.VICTIMS;

public final class VictimTableFields implements VictimFields {

	@Override
	public Table<? extends Record> table() {
		return VICTIMS;
	}

	@Override
	public Field<Victim.VictimType> victimType() {
		return VICTIMS.TYPE;
	}

	@Override
	public Field<UUID> victimUuid() {
		return VICTIMS.UUID;
	}

	@Override
	public Field<NetworkAddress> victimAddress() {
		return VICTIMS.ADDRESS;
	}

	@Override
	public String toString() {
		return "VictimTableFields";
	}
}
