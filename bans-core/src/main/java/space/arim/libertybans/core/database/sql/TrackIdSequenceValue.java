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

import org.jooq.DSLContext;
import org.jooq.Field;
import space.arim.libertybans.api.punish.EscalationTrack;

import static org.jooq.impl.DSL.castNull;
import static org.jooq.impl.DSL.val;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_TRACK_IDS;
import static space.arim.libertybans.core.schema.tables.Tracks.TRACKS;

public final class TrackIdSequenceValue extends SequenceValue<Integer> {

	public TrackIdSequenceValue(DSLContext context) {
		super(context, LIBERTYBANS_TRACK_IDS);
	}

	public Field<Integer> retrieveTrackId(EscalationTrack escalationTrack) {
		if (escalationTrack == null) {
			return castNull(Integer.class);
		}
		return new RetrieveOrGenerate(
				TRACKS, TRACKS.ID,
				TRACKS.NAMESPACE.eq(escalationTrack.getNamespace())
						.and(TRACKS.VALUE.eq(escalationTrack.getValue())),
				(newId) -> {
					context
							.insertInto(TRACKS)
							.columns(TRACKS.ID, TRACKS.NAMESPACE, TRACKS.VALUE)
							.values(newId, val(escalationTrack.getNamespace()), val(escalationTrack.getValue()))
							.execute();
				}
		).execute();
	}

}
