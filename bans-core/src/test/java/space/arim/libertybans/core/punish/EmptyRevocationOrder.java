/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.RevocationOrder;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.List;
import java.util.Optional;

public final class EmptyRevocationOrder implements RevocationOrder, EnforcementOpts.Factory {

	private final FactoryOfTheFuture futuresFactory;

	public EmptyRevocationOrder(FactoryOfTheFuture futuresFactory) {
		this.futuresFactory = futuresFactory;
	}

	@Override
	public Optional<Long> getID() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<PunishmentType> getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<List<Victim>> getVictims() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ReactionStage<Boolean> undoPunishment(EnforcementOptions enforcementOptions) {
		return futuresFactory.completedFuture(false);
	}

	@Override
	public ReactionStage<Optional<Punishment>> undoAndGetPunishment(EnforcementOptions enforcementOptions) {
		return futuresFactory.completedFuture(Optional.empty());
	}
}
