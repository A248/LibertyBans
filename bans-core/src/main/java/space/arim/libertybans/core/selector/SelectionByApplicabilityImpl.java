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

package space.arim.libertybans.core.selector;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.SelectionByApplicability;
import space.arim.libertybans.core.database.sql.ApplicableViewFields;
import space.arim.libertybans.core.database.sql.DeserializedVictim;
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.VictimCondition;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.StrictLinks.STRICT_LINKS;

public final class SelectionByApplicabilityImpl extends SelectionBaseSQL implements SelectionByApplicability {

	private final UUID uuid;
	private final NetworkAddress address;
	private final AddressStrictness strictness;
	private final boolean potentialNewEntrant;

	SelectionByApplicabilityImpl(Details details, SelectionResources resources,
								 UUID uuid, NetworkAddress address, AddressStrictness strictness,
								 boolean potentialNewEntrant) {
		super(details, resources);
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
		this.strictness = Objects.requireNonNull(strictness, "strictness");
		this.potentialNewEntrant = potentialNewEntrant;
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

	/*
	If the potentialNewEntrant flag is set, our task becomes significantly more complicated.
	We now have to account for the enforcement of non-lenient address strictness by acting as if
	the user had their IP address recorded already. This means attaching additional predication
	to scans for IP-based punishments and patching over the applicability links we are accustomed
	to having the database account for.

	Thanks to SnakeAmazing for providing this marvelous intellectual puzzle.


	Note that some queries using OR are turned into unions. This is because database engines famously
	fumble on OR with complicated queries, as they prefer to traverse the join graph once rather than
	multiple times from multiple directions.
	 */

	@Override
	Query<?> requestQuery(QueryParameters parameters) {
		org.slf4j.LoggerFactory.getLogger(getClass())
				.info("LOGISTICS strictness/potentialNewEntrant = " + strictness + '/' + potentialNewEntrant);
		PunishmentFields fields;
		Table<?> table;
		Table<?> tableUnion = null;
		Condition victimCondUnion = null;
		Condition victimCond = switch (strictness) {
			case LENIENT -> {
				fields = requestSimpleView();
				table = fields.table();
				yield new VictimCondition(fields).simplyMatches(uuid, address);
			}
			case NORMAL -> {
				if (potentialNewEntrant) {
					// Compute by hand by selecting addresses. Then pipeline into "lenient" style query
					// This is the best way according to execution plans on MariaDB
					Set<NetworkAddress> addresses = parameters.context()
							.select(ADDRESSES.ADDRESS)
							.from(ADDRESSES)
							.where(ADDRESSES.UUID.eq(uuid))
							.fetchSet(ADDRESSES.ADDRESS);
					if (!(addresses instanceof HashSet<NetworkAddress>)) {
						addresses = new HashSet<>(addresses);
					}
					addresses.add(address);

					fields = requestSimpleView();
					table = fields.table();
					yield new VictimCondition(fields).matchesUUIDOrAddresses(uuid, addresses);
				}
				ApplicableViewFields applView = requestApplicableView();
				fields = applView;
				table = fields.table();
				yield applView.uuid().eq(uuid);
			}
			case STERN -> {
				ApplicableViewFields applView = requestApplicableView();
				fields = applView;
				if (potentialNewEntrant) {
					/*
					SELECT * FROM appl
					WHERE appl.uuid = uuid													# NORMAL
					OR victim_type != 'PLAYER' AND (
					    appl.address = address												# NORMAL + STERN
					    OR strict_links.uuid2 IS NOT NULL AND strict_links.uuid2 = uuid		# STERN
					)

					Note: The last predicate must use appl.address and not victim_address
					Exercise for understanding: Why?

					yield or(
							applView.uuid().eq(uuid),
							applView.victimType().notEqual(inline(VictimType.PLAYER)).and(or(
									STRICT_LINKS.UUID2.isNotNull().and(STRICT_LINKS.UUID2.eq(uuid)),
									applView.address().eq(address)
							))
					);
					- Turn OR into union to help database optimize.
					- Also, IS NOT NULL on one union branch implies INNER JOIN for that query.
					Thus:

					SELECT * FROM appl INNER JOIN strict_links
					    ON appl.uuid = strict_links.uuid1
					    WHERE victim_type != 'PLAYER' AND strict_links.uuid2 = uuid
					UNION
					SELECT * FROM appl
					    WHERE appl.uuid = uuid OR victim_type != 'PLAYER' AND appl.address = address
					 */
					tableUnion = applView.table();
					victimCondUnion = applView.uuid().eq(uuid).or(
							applView.victimType().notEqual(inline(VictimType.PLAYER)).and(applView.address().eq(address))
					);
					table = applView
							.table()
							.innerJoin(STRICT_LINKS)
							.on(applView.uuid().eq(STRICT_LINKS.UUID1));
					yield STRICT_LINKS.UUID2.eq(uuid).and(applView.victimType().notEqual(inline(VictimType.PLAYER)));
				}
				/*
				SELECT * FROM appl INNER JOIN strict_links ON appl.uuid = strict_links.uuid1
				WHERE
					appl.uuid = strict_links.uuid1 = uuid						# NORMAL
					OR victim_type != 'PLAYER' AND strict_links.uuid2 = uuid	# STERN

				Turn OR into union to help database optimize. Thus:

				SELECT * FROM appl INNER JOIN strict_links
				    ON appl.uuid = strict_links.uuid1
				    WHERE victim_type != 'PLAYER' AND strict_links.uuid2 = uuid
				UNION
				SELECT * FROM appl WHERE appl.uuid = uuid
				 */
				tableUnion = applView.table();
				victimCondUnion = applView.uuid().eq(uuid);
				table = applView
						.table()
						.innerJoin(STRICT_LINKS)
						.on(applView.uuid().eq(STRICT_LINKS.UUID1));
				yield STRICT_LINKS.UUID2.eq(uuid).and(applView.victimType().notEqual(inline(VictimType.PLAYER)));
			}
			case STRICT -> {
				ApplicableViewFields applView = requestApplicableView();
				fields = applView;
				if (potentialNewEntrant) {
					/*
					SELECT * FROM appl LEFT JOIN strict_links ON appl.uuid = strict_links.uuid1
					WHERE
						appl.uuid = uuid													# NORMAL
						OR appl.address = address											# NORMAL + STRICT
						OR strict_links.uuid2 IS NOT NULL AND strict_links.uuid2 = uuid		# STRICT

					- Turn OR into union to help database optimize.
					- Also, IS NOT NULL on one union branch implies INNER JOIN for that query.
					Thus:

					SELECT * FROM appl INNER JOIN strict_links
					    ON appl.uuid = strict_links.uuid1
					    WHERE strict_links.uuid2 = uuid
					UNION
					SELECT * FROM appl
					    WHERE appl.uuid = uuid OR appl.address = address
					 */
					tableUnion = applView.table();
					victimCondUnion = applView.uuid().eq(uuid).or(applView.address().eq(address));
				}
				table = applView
						.table()
						.innerJoin(STRICT_LINKS)
						.on(applView.uuid().eq(STRICT_LINKS.UUID1));
				yield STRICT_LINKS.UUID2.eq(uuid);
			}
		};
		assert table != null;
		return new QueryBuilder(parameters, fields) {
			@Override
			List<Field<?>> victimColumns(PunishmentFields fields) {
				return List.of(
						fields.victimType(), fields.victimUuid(), fields.victimAddress()
				);
			}

			@Override
			Victim victimFromRecord(Record record, PunishmentFields fields) {
				return new DeserializedVictim(
						record.get(fields.victimUuid()),
						record.get(fields.victimAddress())
				).victim(
						record.get(fields.victimType())
				);
			}

			@Override
			boolean mightRepeatIds() {
				return strictness != AddressStrictness.LENIENT;
			}
		}.constructSelect(table, tableUnion, victimCond, victimCondUnion);
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
