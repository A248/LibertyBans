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
import space.arim.libertybans.api.select.SelectionPredicate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

abstract class SelectionBaseImpl implements SelectionBase {

	private final Details details;

	SelectionBaseImpl(Details details) {
		this.details = details;
	}

	record Details(SelectionPredicate<PunishmentType> types,
				   SelectionPredicate<Operator> operators, SelectionPredicate<ServerScope> scopes,
				   SelectionPredicate<Optional<EscalationTrack>> escalationTracks,
				   boolean selectActiveOnly, int skipCount, int limitToRetrieve,
				   Instant seekAfterStartTime, long seekAfterId, Instant seekBeforeStartTime, long seekBeforeId) {

		Details {
			Objects.requireNonNull(types, "types");
			Objects.requireNonNull(operators, "operators");
			Objects.requireNonNull(scopes, "scopes");
			Objects.requireNonNull(escalationTracks, "escalationTracks");
			Objects.requireNonNull(seekAfterStartTime, "seekAfterStartTime");
			Objects.requireNonNull(seekBeforeStartTime, "seekBeforeStartTime");
			// Zero-out seek IDs if start time is unset, so that equals and hashCode function reliably
			seekAfterId = (seekAfterStartTime.equals(Instant.EPOCH) ? 0 : seekAfterId);
			seekBeforeId = (seekBeforeStartTime.equals(Instant.MAX) ? 0 : seekBeforeId);
		}

	}

	@Override
	public SelectionPredicate<PunishmentType> getTypes() {
		return details.types;
	}

	@Override
	public SelectionPredicate<Operator> getOperators() {
		return details.operators;
	}

	@Override
	public SelectionPredicate<ServerScope> getScopes() {
		return details.scopes;
	}

	@Override
	public SelectionPredicate<Optional<EscalationTrack>> getEscalationTracks() {
		return details.escalationTracks;
	}

	@Override
	public boolean selectActiveOnly() {
		return details.selectActiveOnly;
	}

	@Override
	public int skipCount() {
		return details.skipCount;
	}

	@Override
	public int limitToRetrieve() {
		return details.limitToRetrieve;
	}

	@Override
	public Instant seekAfterStartTime() {
		return details.seekAfterStartTime;
	}

	@Override
	public long seekAfterId() {
		return details.seekAfterId;
	}

	@Override
	public Instant seekBeforeStartTime() {
		return details.seekBeforeStartTime;
	}

	@Override
	public long seekBeforeId() {
		return details.seekBeforeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SelectionBaseImpl that = (SelectionBaseImpl) o;
		return details.equals(that.details);
	}

	@Override
	public int hashCode() {
		return details.hashCode();
	}

	@Override
	public String toString() {
		return "SelectionBaseImpl{" +
				"details=" + details +
				'}';
	}

}
