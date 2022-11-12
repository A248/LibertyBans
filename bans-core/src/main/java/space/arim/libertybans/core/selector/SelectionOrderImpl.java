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

package space.arim.libertybans.core.selector;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class SelectionOrderImpl implements SelectionOrder {

	private transient final SelectorImpl selector;

	private final SelectionPredicate<PunishmentType> types;
	private final SelectionPredicate<Victim> victims;
	private final SelectionPredicate<Operator> operators;
	private final SelectionPredicate<ServerScope> scopes;
	private final boolean selectActiveOnly;
	private final int skipCount;
	private final int limitToRetrieve;
	private final Instant seekAfterStartTime;
	private final long seekAfterId;
	private final Instant seekBeforeStartTime;
	private final long seekBeforeId;

	SelectionOrderImpl(SelectorImpl selector,
					   SelectionPredicate<PunishmentType> types, SelectionPredicate<Victim> victims,
					   SelectionPredicate<Operator> operators, SelectionPredicate<ServerScope> scopes,
					   boolean selectActiveOnly, int skipCount, int limitToRetrieve,
					   Instant seekAfterStartTime, long seekAfterId, Instant seekBeforeStartTime, long seekBeforeId) {
		this.selector = Objects.requireNonNull(selector, "selector");

		this.types = Objects.requireNonNull(types, "types");
		this.victims = Objects.requireNonNull(victims, "victims");
		this.operators = Objects.requireNonNull(operators, "operators");
		this.scopes = Objects.requireNonNull(scopes, "scopes");
		this.selectActiveOnly = selectActiveOnly;
		this.skipCount = skipCount;
		this.limitToRetrieve = limitToRetrieve;
		this.seekAfterStartTime = Objects.requireNonNull(seekAfterStartTime, "seekAfterStartTime");
		this.seekBeforeStartTime = Objects.requireNonNull(seekBeforeStartTime, "seekBeforeStartTime");
		// Zero-out seekAfterId if start time is unset, so that equals and hashCode function reliably
		this.seekAfterId = (seekAfterStartTime.equals(Instant.EPOCH) ? 0 : seekAfterId);
		this.seekBeforeId = (seekBeforeStartTime.equals(Instant.EPOCH) ? 0 : seekBeforeId);
	}

	@Override
	public SelectionPredicate<PunishmentType> getTypes() {
		return types;
	}

	@Override
	public SelectionPredicate<Victim> getVictims() {
		return victims;
	}

	@Override
	public SelectionPredicate<Operator> getOperators() {
		return operators;
	}

	@Override
	public SelectionPredicate<ServerScope> getScopes() {
		return scopes;
	}

	@Override
	public boolean selectActiveOnly() {
		return selectActiveOnly;
	}

	@Override
	public int skipCount() {
		return skipCount;
	}

	@Override
	public int limitToRetrieve() {
		return limitToRetrieve;
	}

	@Override
	public Instant seekAfterStartTime() {
		return seekAfterStartTime;
	}

	@Override
	public long seekAfterId() {
		return seekAfterId;
	}

	@Override
	public Instant seekBeforeStartTime() {
		return seekBeforeStartTime;
	}

	@Override
	public long seekBeforeId() {
		return seekBeforeId;
	}

	@Override
	public ReactionStage<Optional<Punishment>> getFirstSpecificPunishment() {
		return selector.getFirstSpecificPunishment(this).thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<List<Punishment>> getAllSpecificPunishments() {
		return selector.getSpecificPunishments(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SelectionOrderImpl that = (SelectionOrderImpl) o;
		return selectActiveOnly == that.selectActiveOnly
				&& skipCount == that.skipCount
				&& limitToRetrieve == that.limitToRetrieve
				&& seekAfterId == that.seekAfterId
				&& seekBeforeId == that.seekBeforeId
				&& types.equals(that.types)
				&& victims.equals(that.victims)
				&& operators.equals(that.operators)
				&& scopes.equals(that.scopes)
				&& seekAfterStartTime.equals(that.seekAfterStartTime)
				&& seekBeforeStartTime.equals(that.seekBeforeStartTime);
	}

	@Override
	public int hashCode() {
		int result = types.hashCode();
		result = 31 * result + victims.hashCode();
		result = 31 * result + operators.hashCode();
		result = 31 * result + scopes.hashCode();
		result = 31 * result + (selectActiveOnly ? 1 : 0);
		result = 31 * result + skipCount;
		result = 31 * result + limitToRetrieve;
		result = 31 * result + seekAfterStartTime.hashCode();
		result = 31 * result + (int) (seekAfterId ^ (seekAfterId >>> 32));
		result = 31 * result + seekBeforeStartTime.hashCode();
		result = 31 * result + (int) (seekBeforeId ^ (seekBeforeId >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "SelectionOrderImpl{" +
				"types=" + types +
				", victims=" + victims +
				", operators=" + operators +
				", scopes=" + scopes +
				", selectActiveOnly=" + selectActiveOnly +
				", skipCount=" + skipCount +
				", limitToRetrieve=" + limitToRetrieve +
				", seekAfterStartTime=" + seekAfterStartTime +
				", seekAfterId=" + seekAfterId +
				", seekBeforeStartTime=" + seekBeforeStartTime +
				", seekBeforeId=" + seekBeforeId +
				'}';
	}
}
