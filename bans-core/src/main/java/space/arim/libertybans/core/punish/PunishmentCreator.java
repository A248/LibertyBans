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
import space.arim.libertybans.core.scope.ScopeType;

import java.time.Instant;
import java.util.UUID;

public interface PunishmentCreator {

	Punishment createPunishment(long id, PunishmentType type, Victim victim,
								Operator operator, String reason,
								ServerScope scope, Instant start, Instant end, EscalationTrack escalationTrack);

	RecordMapper<Record12<
			Long, PunishmentType, Victim.VictimType, UUID, NetworkAddress, Operator, String, String, Instant, Instant, EscalationTrack, ScopeType>,
			Punishment> punishmentMapper();

	RecordMapper<Record11<
			PunishmentType, Victim.VictimType, UUID, NetworkAddress, Operator, String, String, Instant, Instant, EscalationTrack, ScopeType>,
			Punishment> punishmentMapper(long id);

	RecordMapper<Record10<
			Victim.VictimType, UUID, NetworkAddress, Operator, String, String, Instant, Instant, EscalationTrack, ScopeType>,
			Punishment> punishmentMapper(long id, PunishmentType type);

	RecordMapper<Record6<
			String, Instant, String, String, ScopeType, String>,
			Punishment> punishmentMapperForModifications(Punishment oldPunishment);

}
