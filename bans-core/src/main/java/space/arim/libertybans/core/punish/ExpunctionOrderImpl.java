/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.ExpunctionOrder;
import space.arim.omnibus.util.concurrent.ReactionStage;

class ExpunctionOrderImpl implements ExpunctionOrder, EnforcementOpts.Factory {

	private final Revoker revoker;
	private final long id;

	ExpunctionOrderImpl(Revoker revoker, long id) {
		this.revoker = revoker;
		this.id = id;
	}

	@Override
	public long getID() {
		return id;
	}

	@Override
	public ReactionStage<Boolean> expunge(EnforcementOptions enforcementOptions) {
		return revoker.expungeById(id).thenCompose((expunged) -> {
			if (!expunged) {
				return revoker.futuresFactory().completedFuture(false);
			}
			return revoker.enforcement()
					.clearExpunged(id, (EnforcementOpts) enforcementOptions)
					.thenApply((ignore) -> true);
		});
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExpunctionOrderImpl that = (ExpunctionOrderImpl) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return (int) (id ^ (id >>> 32));
	}

	@Override
	public String toString() {
		return "ExpunctionOrderImpl{" +
				"id=" + id +
				'}';
	}

}
