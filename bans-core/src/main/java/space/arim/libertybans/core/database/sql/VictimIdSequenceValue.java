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
import space.arim.libertybans.api.Victim;

import static org.jooq.impl.DSL.val;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_VICTIM_IDS;
import static space.arim.libertybans.core.schema.tables.Victims.VICTIMS;

public final class VictimIdSequenceValue extends SequenceValue<Integer> {

	public VictimIdSequenceValue(DSLContext context) {
		super(context, LIBERTYBANS_VICTIM_IDS);
	}

	public Field<Integer> retrieveVictimId(Victim victim) {
		VictimData victimData = FixedVictimData.from(new SerializedVictim(victim));
		return new RetrieveOrGenerate(
				VICTIMS, VICTIMS.ID,
				new VictimCondition(new VictimTableFields()).matchesVictim(victimData),
				(newId) -> {
					context
							.insertInto(VICTIMS)
							.columns(VICTIMS.ID, VICTIMS.TYPE, VICTIMS.UUID, VICTIMS.ADDRESS)
							.values(
									newId,
									val(victimData.type(), VICTIMS.TYPE),
									val(victimData.uuid(), VICTIMS.UUID),
									val(victimData.address(), VICTIMS.ADDRESS)
							)
							.execute();
				}
		).execute();
	}

}
