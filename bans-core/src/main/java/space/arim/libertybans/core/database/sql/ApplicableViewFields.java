/*
 * LibertyBans
 * Copyright © 2024 Anand Beh
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
import org.jooq.Record14;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.core.scope.ScopeType;

import java.time.Instant;
import java.util.UUID;

public record ApplicableViewFields<R extends Record14<
		Long, PunishmentType,
		Victim.VictimType, UUID, NetworkAddress,
		Operator, String, String, Instant, Instant,
		UUID, NetworkAddress, EscalationTrack, ScopeType
		>>(Table<R> applicableView, R fieldSupplier) implements PunishmentFields {

	public ApplicableViewFields(Table<R> applicableView) {
		this(applicableView, applicableView.newRecord());
	}

	@Override
	public Table<? extends Record> table() {
		return applicableView;
	}

	@Override
	public Field<Long> id() {
		return fieldSupplier.field1();
	}

	@Override
	public Field<PunishmentType> type() {
		return fieldSupplier.field2();
	}

	@Override
	public Field<Victim.VictimType> victimType() {
		return fieldSupplier.field3();
	}

	@Override
	public Field<UUID> victimUuid() {
		return fieldSupplier.field4();
	}

	@Override
	public Field<NetworkAddress> victimAddress() {
		return fieldSupplier.field5();
	}

	@Override
	public Field<Operator> operator() {
		return fieldSupplier.field6();
	}

	@Override
	public Field<String> reason() {
		return fieldSupplier.field7();
	}

	@Override
	public Field<String> scope() {
		return fieldSupplier.field8();
	}

	@Override
	public Field<Instant> start() {
		return fieldSupplier.field9();
	}

	@Override
	public Field<Instant> end() {
		return fieldSupplier.field10();
	}

	@Override
	public Field<EscalationTrack> track() {
		return fieldSupplier.field13();
	}

	@Override
	public Field<ScopeType> scopeType() {
		return fieldSupplier.field14();
	}

	public Field<UUID> uuid() {
		return fieldSupplier.field11();
	}

	public Field<NetworkAddress> address() {
		return fieldSupplier.field12();
	}

}
