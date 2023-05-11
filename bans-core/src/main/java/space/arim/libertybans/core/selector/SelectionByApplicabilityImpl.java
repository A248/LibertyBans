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

package space.arim.libertybans.core.selector;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.SelectionByApplicability;
import space.arim.libertybans.core.database.sql.ApplicableViewFields;
import space.arim.libertybans.core.database.sql.DeserializedVictim;
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.VictimCondition;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.jooq.impl.DSL.inline;
import static space.arim.libertybans.core.schema.tables.StrictLinks.STRICT_LINKS;

public final class SelectionByApplicabilityImpl extends SelectionBaseSQL implements SelectionByApplicability {

	private final UUID uuid;
	private final NetworkAddress address;
	private final AddressStrictness strictness;

	SelectionByApplicabilityImpl(Details details, SelectionResources resources,
								 UUID uuid, NetworkAddress address, AddressStrictness strictness) {
		super(details, resources);
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
		this.strictness = Objects.requireNonNull(strictness, "strictness");
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public NetworkAddress getAddress() {
		return address;
	}

	@Override
	public AddressStrictness getAddressStrictness() {
		return strictness;
	}

	@Override
	Query<?> requestQuery(QueryParameters parameters) {
		PunishmentFields fields = null;
		Table<?> table = null;
		Condition additionalPredication = switch (strictness) {
			case LENIENT -> {
				fields = requestSimpleView();
				table = fields.table();
				yield new VictimCondition(fields).simplyMatches(uuid, address);
			}
			case NORMAL -> {
				ApplicableViewFields<?> applView = requestApplicableView();
				fields = applView;
				table = fields.table();
				yield applView.uuid().eq(uuid); // appl.uuid = uuid
			}
			case STERN -> {
				ApplicableViewFields<?> applView = requestApplicableView();
				fields = applView;
				table = fields
						.table()
						.innerJoin(STRICT_LINKS)
						.on(applView.uuid().eq(STRICT_LINKS.UUID1));
				// appl.uuid = strict_links.uuid1 = uuid
				// OR victim_type != 'PLAYER' AND strict_links.uuid2 = uuid
				yield STRICT_LINKS.UUID1.eq(uuid).or(
						STRICT_LINKS.UUID2.eq(uuid).and(applView.victimType().notEqual(inline(VictimType.PLAYER))));
			}
			case STRICT -> {
				ApplicableViewFields<?> applView = requestApplicableView();
				fields = applView;
				table = fields
						.table()
						.innerJoin(STRICT_LINKS)
						.on(applView.uuid().eq(STRICT_LINKS.UUID1));
				// strict_links.uuid2 = uuid
				yield STRICT_LINKS.UUID2.eq(uuid);
			}
		};
		List<Field<?>> additionalColumns = List.of(
				fields.victimType(), fields.victimUuid(), fields.victimAddress()
		);
		assert table != null;
		return new QueryBuilder(parameters, fields, table) {
			@Override
			Victim victimFromRecord(Record record) {
				return new DeserializedVictim(
						record.get(aggregateIfNeeded(fields.victimUuid())),
						record.get(aggregateIfNeeded(fields.victimAddress()))
				).victim(
						record.get(aggregateIfNeeded(fields.victimType()))
				);
			}

			@Override
			boolean mightRepeatIds() {
				return strictness != AddressStrictness.LENIENT;
			}
		}.constructSelect(additionalColumns, additionalPredication);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SelectionByApplicabilityImpl that = (SelectionByApplicabilityImpl) o;
		return uuid.equals(that.uuid) && address.equals(that.address) && strictness == that.strictness;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + uuid.hashCode();
		result = 31 * result + address.hashCode();
		result = 31 * result + strictness.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SelectionByApplicabilityImpl{" +
				"uuid=" + uuid +
				", address=" + address +
				", strictness=" + strictness +
				", types=" + getTypes() +
				", operators=" + getOperators() +
				", scopes=" + getScopes() +
				", selectActiveOnly=" + selectActiveOnly() +
				", skipCount=" + skipCount() +
				", limitToRetrieve=" + limitToRetrieve() +
				", seekAfterStartTime=" + seekAfterStartTime() +
				", seekAfterId=" + seekAfterId() +
				", seekBeforeStartTime=" + seekBeforeStartTime() +
				", seekBeforeId=" + seekBeforeId() +
				'}';
	}

}
