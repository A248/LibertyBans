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

import org.jooq.Condition;
import org.jooq.Field;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.punish.MiscUtil;

import java.util.Objects;
import java.util.UUID;

public final class VictimCondition {

	private final VictimFields fields;

	public VictimCondition(VictimFields fields) {
		this.fields = Objects.requireNonNull(fields, "fields");
	}

	public Condition simplyMatches(Field<UUID> uuid, Field<NetworkAddress> address) {
		// victim_type = PLAYER AND victim_uuid = uuid
		// OR victim_type = ADDRESS AND victim_address = address
		// OR victim_type = COMPOSITE AND (victim_uuid = uuid OR victim_address = address)
		return fields.victimType().eq(Victim.VictimType.PLAYER).and(fields.victimUuid().eq(uuid))
				.or(
						fields.victimType().eq(Victim.VictimType.ADDRESS).and(fields.victimAddress().eq(address))
				).or(
						fields.victimType().eq(Victim.VictimType.COMPOSITE).and(
								fields.victimUuid().eq(uuid).or(fields.victimAddress().eq(address))
						)
				);
	}

	public Condition matchesUUID(Field<UUID> uuid) {
		// (victim_type = PLAYER OR victim_type = COMPOSITE) AND (victim_uuid = uuid)
		return fields.victimType().eq(Victim.VictimType.PLAYER)
				.or(fields.victimType().eq(Victim.VictimType.COMPOSITE))
				.and(fields.victimUuid().eq(uuid));
	}

	private Condition matchesVictimData(VictimData victim) {
		Victim.VictimType victimType = victim.type();
		switch (victimType) {
		case PLAYER:
			return fields.victimUuid().eq(victim.uuid());
		case ADDRESS:
			return fields.victimAddress().eq(victim.address());
		case COMPOSITE:
			return fields.victimUuid().eq(victim.uuid()).and(fields.victimAddress().eq(victim.address()));
		default:
			throw MiscUtil.unknownVictimType(victimType);
		}
	}

	public Condition matchesVictim(VictimData victimData) {
		return fields.victimType().eq(victimData.type()).and(matchesVictimData(victimData));
	}

	public Condition matchesVictim(Victim victim) {
		return matchesVictim(new SerializedVictim(victim));
	}

	@Override
	public String
	toString() {
		return "VictimCondition{" +
				"fields.type()=" + fields.victimType() +
				", fields.victimUuid()=" + fields.victimUuid() +
				", fields.victimAddress()=" + fields.victimAddress() +
				'}';
	}
}
