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

package space.arim.libertybans.core.selector;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionBase;
import space.arim.libertybans.api.select.SelectionBuilderBase;
import space.arim.libertybans.api.select.SelectionPredicate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

abstract class SelectionBuilderBaseImpl<B extends SelectionBuilderBase<B, S>, S extends SelectionBase>
		implements SelectionBuilderBase<B, S> {

	private SelectionPredicate<PunishmentType> types = SelectionPredicate.matchingAll();
	private SelectionPredicate<Operator> operators = SelectionPredicate.matchingAll();
	private SelectionPredicate<ServerScope> scopes = SelectionPredicate.matchingAll();
	private SelectionPredicate<Optional<EscalationTrack>> escalationTracks = SelectionPredicate.matchingAll();
	private boolean selectActiveOnly = true;
	private int skipCount;
	private int limitToRetrieve;
	private Instant seekAfterStartTime = Instant.EPOCH;
	private long seekAfterId;
	private Instant seekBeforeStartTime = Instant.MAX;
	private long seekBeforeId;

	abstract B yieldSelf();

	abstract S buildWith(SelectionBaseImpl.Details details);

	@Override
	public B types(SelectionPredicate<PunishmentType> types) {
		this.types = Objects.requireNonNull(types, "types");
		return yieldSelf();
	}

	@Override
	public B operators(SelectionPredicate<Operator> operators) {
		this.operators = Objects.requireNonNull(operators, "operator");
		return yieldSelf();
	}

	@Override
	public B scopes(SelectionPredicate<ServerScope> scopes) {
		this.scopes = Objects.requireNonNull(scopes, "scopes");
		return yieldSelf();
	}

	@Override
	public B escalationTracks(SelectionPredicate<Optional<EscalationTrack>> escalationTracks) {
		this.escalationTracks = Objects.requireNonNull(escalationTracks, "escalationTracks");
		return yieldSelf();
	}

	@Override
	public B selectActiveOnly(boolean selectActiveOnly) {
		this.selectActiveOnly = selectActiveOnly;
		return yieldSelf();
	}

	@Override
	public B skipFirstRetrieved(int skipCount) {
		if (skipCount < 0) {
			throw new IllegalArgumentException("Skip count must be non-negative");
		}
		this.skipCount = skipCount;
		return yieldSelf();
	}

	@Override
	public B limitToRetrieve(int limitToRetrieve) {
		if (limitToRetrieve < 0) {
			throw new IllegalArgumentException("Maximum to retrieve must be non-negative");
		}
		this.limitToRetrieve = limitToRetrieve;
		return yieldSelf();
	}

	@Override
	public B seekAfter(Instant minimumStartTime, long minimumId) {
		this.seekAfterStartTime = Objects.requireNonNull(minimumStartTime, "minimumStartTime");
		this.seekAfterId = minimumId;
		return yieldSelf();
	}

	@Override
	public B seekBefore(Instant maximumStartTime, long maximumId) {
		this.seekBeforeStartTime = Objects.requireNonNull(maximumStartTime, "maximumStartTime");
		this.seekBeforeId = maximumId;
		return yieldSelf();
	}

	@Override
	public S build() {
		return buildWith(new SelectionBaseImpl.Details(
				types, operators, scopes, escalationTracks, selectActiveOnly, skipCount, limitToRetrieve,
				seekAfterStartTime, seekAfterId, seekBeforeStartTime, seekBeforeId
		));
	}

}
