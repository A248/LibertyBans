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
 * Enforcement of a punishment across an entire network, as opposed to a single server,
 * hence "global"
 *
 */
public interface GlobalEnforcement extends Runnable {

	CentralisedFuture<Void> enforce(Punishment punishment, EnforcementOpts enforcementOptions);

	CentralisedFuture<Void> unenforce(Punishment punishment, EnforcementOpts enforcementOptions);

	CentralisedFuture<Void> unenforce(long id, PunishmentType type, EnforcementOpts enforcementOptions);

	CentralisedFuture<Void> clearExpunged(long id);

	CentralisedFuture<Void> updateDetails(Punishment punishment);

}
