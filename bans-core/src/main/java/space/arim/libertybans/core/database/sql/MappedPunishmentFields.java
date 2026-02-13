/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.core.scope.ScopeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MappedPunishmentFields(PunishmentFields inner, Mapper mapper) implements PunishmentFields {

    @Override
    public Field<Long> id() {
        return mapper.map(inner.id());
    }

    @Override
    public Field<PunishmentType> type() {
        return mapper.map(inner.type());
    }

    @Override
    public Field<Operator> operator() {
        return mapper.map(inner.operator());
    }

    @Override
    public Field<String> reason() {
        return mapper.map(inner.reason());
    }

    @Override
    public Field<Instant> start() {
        return mapper.map(inner.start());
    }

    @Override
    public Field<Instant> end() {
        return mapper.map(inner.end());
    }

    @Override
    public Field<EscalationTrack> track() {
        return mapper.map(inner.track());
    }

    @Override
    public Field<Victim.VictimType> victimType() {
        return mapper.map(inner.victimType());
    }

    @Override
    public Field<UUID> victimUuid() {
        return mapper.map(inner.victimUuid());
    }

    @Override
    public Field<NetworkAddress> victimAddress() {
        return mapper.map(inner.victimAddress());
    }

    @Override
    public Table<? extends Record> table() {
        return inner.table();
    }

    @Override
    public Field<String> scope() {
        return mapper.map(inner.scope());
    }

    @Override
    public Field<ScopeType> scopeType() {
        return mapper.map(inner.scopeType());
    }

    public interface Mapper {

        <T> Field<T> map(Field<T> original);

        default ArrayList<Field<?>> mapMultiple(List<Field<?>> originals) {
            ArrayList<Field<?>> mapped = new ArrayList<>(10 + originals.size());
            for (Field<?> original : originals) {
                mapped.add(map(original));
            }
            return mapped;
        }
    }
}
