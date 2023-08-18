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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.Record10;
import org.jooq.Record11;
import org.jooq.Record12;
import org.jooq.Record6;
import org.jooq.RecordMapper;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.sql.DeserializedVictim;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.scope.ScopeType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Singleton
public class SecurePunishmentCreator implements PunishmentCreator {

	private final InternalScopeManager scopeManager;
	private final Provider<InternalRevoker> revoker;
	private final Provider<GlobalEnforcement> enforcement;
	private final Provider<Modifier> modifier;

	@Inject
	public SecurePunishmentCreator(InternalScopeManager scopeManager, Provider<InternalRevoker> revoker,
								   Provider<GlobalEnforcement> enforcement, Provider<Modifier> modifier) {
		this.scopeManager = scopeManager;
		this.revoker = revoker;
		this.enforcement = enforcement;
		this.modifier = modifier;
	}

	InternalRevoker revoker() {
		return revoker.get();
	}

	GlobalEnforcement enforcement() {
		return enforcement.get();
	}

	Modifier modifer() {
		return modifier.get();
	}

	@Override
	public Punishment createPunishment(long id, PunishmentType type, Victim victim, Operator operator, String reason,
									   ServerScope scope, Instant start, Instant end, EscalationTrack escalationTrack) {
		return new SecurePunishment(this, id, type, victim, operator, reason, scope, start, end, escalationTrack);
	}

	@Override
	public RecordMapper<Record12<
			Long, PunishmentType, Victim.VictimType, UUID, NetworkAddress, Operator, String, String, Instant, Instant, EscalationTrack, ScopeType>,
			Punishment> punishmentMapper() {
		return (record) -> {
			Victim victim = new DeserializedVictim(
					record.value4(), record.value5()
			).victim(record.value3());
			ServerScope scope = scopeManager.deserialize(
					record.value12(), record.value8()
			);
			return new SecurePunishment(
					SecurePunishmentCreator.this,
					record.value1(), record.value2(), // id, type
					victim, record.value6(), record.value7(), // victim, operator, reason
					scope, record.value9(), record.value10(), record.value11() // scope, start, end, track
			);
		};
	}

	@Override
	public RecordMapper<Record11<
			PunishmentType, Victim.VictimType, UUID, NetworkAddress, Operator, String, String, Instant, Instant, EscalationTrack, ScopeType>,
			Punishment> punishmentMapper(long id) {
		return (record) -> {
			Victim victim = new DeserializedVictim(
					record.value3(), record.value4()
			).victim(record.value2());
			ServerScope scope = scopeManager.deserialize(
					record.value11(), record.value7()
			);
			return new SecurePunishment(
					SecurePunishmentCreator.this,
					id, /* type */ record.value1(), victim,
					record.value5(), record.value6(), // operator, reason
					scope, record.value8(), record.value9(), record.value10() // scope, start, end, track
			);
		};
	}

	@Override
	public RecordMapper<Record10<
			Victim.VictimType, UUID, NetworkAddress, Operator, String, String, Instant, Instant, EscalationTrack, ScopeType>,
			Punishment> punishmentMapper(long id, PunishmentType type) {
		return (record) -> {
			Victim victim = new DeserializedVictim(
					record.value2(), record.value3()
			).victim(record.value1());
			ServerScope scope = scopeManager.deserialize(
					record.value10(), record.value6()
			);
			return new SecurePunishment(
					SecurePunishmentCreator.this,
					id, type, victim,
					record.value4(), record.value5(), // operator, reason
					scope, record.value7(), record.value8(), record.value9() // scope, start, end, track
			);
		};
	}

	@Override
	public RecordMapper<Record6<
			String, Instant, String, String, ScopeType, String>,
			Punishment> punishmentMapperForModifications(Punishment oldPunishment) {
		return (record) -> {
			EscalationTrack escalationTrack;
			{
				String trackNamespace = record.value3();
				String trackValue = record.value4();
				if (trackNamespace == null && trackValue == null) {
					escalationTrack = null;
				} else {
					escalationTrack = EscalationTrack.create(trackNamespace, trackValue);
				}
			}
			ServerScope scope = scopeManager.deserialize(
					Objects.requireNonNullElse(record.value5(), ScopeType.GLOBAL),
					record.value6()
			);
			return new SecurePunishment(
					SecurePunishmentCreator.this,
					oldPunishment.getIdentifier(), oldPunishment.getType(),
					oldPunishment.getVictim(), oldPunishment.getOperator(), record.value1(),
					scope, oldPunishment.getStartDate(), record.value2(),
					escalationTrack
			);
		};
	}

}
