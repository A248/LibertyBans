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

package space.arim.libertybans.api.punish;

import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.util.Optional;

/**
 * A punishment ready to be created. Does not yet have an ID or start and end
 * time, but does contain a duration.
 *
 */
public interface DraftPunishment extends PunishmentBase, DraftSanction {

	/**
	 * Gets the duration of this draft punishment
	 * 
	 * @return the duration
	 */
	Duration getDuration();

	/*
	Redeclare following methods from DraftSanction for 1.0 API compatibility
	 */

	@Override
	default ReactionStage<Optional<Punishment>> enactPunishment() {
		return enactPunishment(enforcementOptionsBuilder().build());
	}

	@Override
	ReactionStage<Optional<Punishment>> enactPunishment(EnforcementOptions enforcementOptions);

	/**
	 * Creates a new {@link DraftPunishmentBuilder}, copying all properties of this draft.
	 *
	 * @return A new builder
	 */
	DraftPunishmentBuilder toBuilder();

	@Override
	boolean equals(Object object);

}
