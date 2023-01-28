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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

/**
 * Enforcer of punishments on a single backend server
 *
 */
public interface LocalEnforcer {

	/**
	 * Enforces a punishment. No punishment synchronization is performed (with regard to multiple
	 * instances of LibertyBans). For synchronization see {@link GlobalEnforcement}
	 * 
	 * @param punishment the punishment
	 * @param enforcementOptions the enforcement options
	 * @return a future completed when enforcement is finished
	 */
	CentralisedFuture<Void> enforceWithoutSynchronization(Punishment punishment, EnforcementOpts enforcementOptions);

	/**
	 * Unenforces a punishment. No punishment synchronization is performed (with regard to multiple
	 * instances of LibertyBans). For synchronization see {@link GlobalEnforcement}
	 *
	 * @param punishment the punishment undone
	 * @param enforcementOptions the enforcement options
	 * @return a future completed when unenforcement is finished
	 */
	CentralisedFuture<Void> unenforceWithoutSynchronization(Punishment punishment, EnforcementOpts enforcementOptions);

	/**
	 * Unenforces a punishment. No punishment synchronization is performed (with regard to multiple
	 * instances of LibertyBans). For synchronization see {@link GlobalEnforcement}
	 *
	 * @param id the id of the punishment undone
	 * @param type the type of the punishment undone
	 * @param enforcementOptions the enforcement options
	 * @return a future completed when unenforcement is finished
	 */
	CentralisedFuture<Void> unenforceWithoutSynchronization(long id, PunishmentType type, EnforcementOpts enforcementOptions);

	/**
	 * Clears an expunged punishment. No punishment synchronization is performed (with regard to multiple
	 * instances of LibertyBans). For synchronization see {@link GlobalEnforcement}
	 *
	 * @param id the id of the punishment expunged
	 * @return a future completed once cleared
	 */
	CentralisedFuture<Void> clearExpungedWithoutSynchronization(long id);

	/**
	 * Updates punishment details. No punishment synchronization is performed (with regard to multiple
	 * instances of LibertyBans). For synchronization see {@link GlobalEnforcement}
	 *
	 * @param punishment the new punishment received
	 * @return a future completed once updated
	 */
	CentralisedFuture<Void> updateDetailsWithoutSynchronization(Punishment punishment);

	/**
	 * Updates punishment details. No punishment synchronization is performed (with regard to multiple
	 * instances of LibertyBans). For synchronization see {@link GlobalEnforcement}
	 *
	 * @param id the id of the punishment updated
	 * @return a future completed once updated
	 */
	CentralisedFuture<Void> updateDetailsWithoutSynchronization(long id);

}
