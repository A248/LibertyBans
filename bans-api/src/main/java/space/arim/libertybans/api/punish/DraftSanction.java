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

import java.util.Optional;

/**
 * A punishment ready to be created through some means
 *
 */
public interface DraftSanction extends SanctionBase, EnforcementOptionsFactory {

	/**
	 * Enacts this sanction, adding it to the database as a punishment, then enforces it. <br>
	 * <br>
	 * If the punishment type is a ban or mute, and there is already an active ban
	 * or mute for the victim, the future will yield an empty optional. See
	 * {@link space.arim.libertybans.api.punish} for a description of active and
	 * historical punishments.
	 *
	 * @return a future which yields the punishment or an empty optional if there
	 *         was a conflict
	 */
	default ReactionStage<Optional<Punishment>> enactPunishment() {
		return enactPunishment(enforcementOptionsBuilder().build());
	}

	/**
	 * Enacts this sanction, adding it to the database as a punishment, then enforces it
	 * according to the given options. <br>
	 * <br>
	 * If the punishment type is a ban or mute, and there is already an active ban
	 * or mute for the victim, the future will yield an empty optional. See
	 * {@link space.arim.libertybans.api.punish} for a description of active and
	 * historical punishments.
	 *
	 * @param enforcementOptions the enforcement options. Can be used to disable enforcement entirely
	 * @return a future which yields the punishment or an empty optional if there
	 *         was a conflict
	 */
	ReactionStage<Optional<Punishment>> enactPunishment(EnforcementOptions enforcementOptions);

	/**
	 * Whether this draft sanction is equal to another. The other draft sanction
	 * must have the same details as this one
	 *
	 * @param object the object to determine equality with
	 * @return true if equal, false otherwise
	 */
	@Override
	boolean equals(Object object);

}
