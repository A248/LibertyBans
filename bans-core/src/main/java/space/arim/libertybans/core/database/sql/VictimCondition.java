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

package space.arim.libertybans.core.database.sql;

import org.jooq.Condition;
import org.jooq.Field;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Victim;

import java.util.Objects;
import java.util.UUID;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.noCondition;
import static space.arim.libertybans.api.CompositeVictim.WILDCARD_ADDRESS;
import static space.arim.libertybans.api.CompositeVictim.WILDCARD_UUID;

public final class VictimCondition implements MultiFieldCriterion<Victim> {

	private final VictimFields fields;

	public VictimCondition(VictimFields fields) {
		this.fields = Objects.requireNonNull(fields, "fields");
	}

	public Condition simplyMatches(UUID uuid, NetworkAddress address) {
		Condition matchesUUID = fields.victimUuid().eq(uuid);
		Condition matchesAddress = fields.victimAddress().eq(address);
		// victim_type = PLAYER AND victim_uuid = uuid
		// OR victim_type = ADDRESS AND victim_address = address
		// OR victim_type = COMPOSITE AND (victim_uuid = uuid OR victim_address = address)
		return fields.victimType().eq(inline(Victim.VictimType.PLAYER)).and(matchesUUID)
				.or(
						fields.victimType().eq(inline(Victim.VictimType.ADDRESS)).and(matchesAddress)
				).or(
						fields.victimType().eq(inline(Victim.VictimType.COMPOSITE)).and(matchesUUID.or(matchesAddress))
				);
	}

	public Condition matchesUUID(Field<UUID> uuid) {
		// (victim_uuid = uuid) AND (victim_type = PLAYER OR victim_type = COMPOSITE)
		return fields.victimUuid().eq(uuid).and(
				fields.victimType().eq(inline(Victim.VictimType.PLAYER))
						.or(fields.victimType().eq(inline(Victim.VictimType.COMPOSITE)))
		);
	}

	public Condition matchesVictim(VictimData victim) {
		Condition matchesData = switch (victim.type()) {
			case PLAYER -> fields.victimUuid().eq(victim.uuid());
			case ADDRESS -> fields.victimAddress().eq(victim.address());
			case COMPOSITE -> {
				UUID uuid = victim.uuid();
				NetworkAddress address = victim.address();
				Condition uuidCondition = (uuid.equals(WILDCARD_UUID)) ?
						noCondition() : fields.victimUuid().eq(uuid);
				Condition addressCondition = (address.equals(WILDCARD_ADDRESS)) ?
						noCondition() : fields.victimAddress().eq(address);
				yield uuidCondition.and(addressCondition);
			}
		};
		return fields.victimType().eq(inline(victim.type())).and(matchesData);
	}

	@Override
	public Condition matchesValue(Victim victim) {
		return matchesVictim(new SerializedVictim(victim));
	}

	@Override
	public String toString() {
		return "VictimCondition{" +
				"fields.type()=" + fields.victimType() +
				", fields.victimUuid()=" + fields.victimUuid() +
				", fields.victimAddress()=" + fields.victimAddress() +
				'}';
	}

}
