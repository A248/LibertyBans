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
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Optional;

public final class EmptyRevocationOrder implements RevocationOrder {

	private final FactoryOfTheFuture futuresFactory;

	public EmptyRevocationOrder(FactoryOfTheFuture futuresFactory) {
		this.futuresFactory = futuresFactory;
	}

	@Override
	public Optional<Integer> getID() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<PunishmentType> getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Victim> getVictim() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ReactionStage<Boolean> undoPunishment() {
		return futuresFactory.completedFuture(false);
	}

	@Override
	public ReactionStage<Boolean> undoPunishmentWithoutUnenforcement() {
		return futuresFactory.completedFuture(false);
	}

	@Override
	public ReactionStage<Optional<Punishment>> undoAndGetPunishment() {
		return futuresFactory.completedFuture(Optional.empty());
	}

	@Override
	public ReactionStage<Optional<Punishment>> undoAndGetPunishmentWithoutUnenforcement() {
		return futuresFactory.completedFuture(Optional.empty());
	}
}
