/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.punish;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.RevocationOrder;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class RevocationOrderImpl implements RevocationOrder, EnforcementOpts.Factory {

	private final Revoker revoker;
	private final long id;
	private final PunishmentType type;
	private final List<Victim> victims;

	private RevocationOrderImpl(Revoker revoker, long id, PunishmentType type, List<Victim> victims) {
		this.revoker = revoker;
		this.id = id;
		this.type = type;
		this.victims = victims;
	}

	RevocationOrderImpl(Revoker revoker, long id) {
		this(revoker, id, null, null);
		assert getApproach() == Approach.ID;
	}

	RevocationOrderImpl(Revoker revoker, long id, PunishmentType type) {
		this(revoker, id, Objects.requireNonNull(type, "type"), null);
		assert getApproach() == Approach.ID_TYPE;
	}

	RevocationOrderImpl(Revoker revoker, PunishmentType type, List<Victim> victims) {
		this(revoker, -1, type, List.copyOf(victims));
		assert getApproach() == Approach.TYPE_VICTIM;
	}

	private enum Approach {
		ID,
		ID_TYPE,
		TYPE_VICTIM,
	}

	private Approach getApproach() {
		if (type == null) {
			return Approach.ID;
		}
		if (victims != null) {
			return Approach.TYPE_VICTIM;
		}
		return Approach.ID_TYPE;
	}

	@Override
	public Optional<Long> getID() {
		if (id == -1L) {
			return Optional.empty();
		}
		return Optional.of(id);
	}

	@Override
	public Optional<PunishmentType> getType() {
		return Optional.ofNullable(type);
	}

	@Override
	public Optional<List<Victim>> getVictims() {
		return Optional.ofNullable(victims);
	}

	@Override
	public ReactionStage<Boolean> undoPunishment(EnforcementOptions enforcementOptions) {
		// Unenforcement needs both an ID and a type
		return switch (getApproach()) {
			case ID -> revoker.undoPunishmentById(id).thenCompose((type) -> {
				if (type == null) {
					return revoker.futuresFactory().completedFuture(false);
				}
				return unenforceAndReturnTrue(id, type, enforcementOptions);
			});
			case ID_TYPE -> revoker.undoPunishmentByIdAndType(id, type).thenCompose((revoked) -> {
				if (!revoked) {
					return revoker.futuresFactory().completedFuture(false);
				}
				return unenforceAndReturnTrue(id, type, enforcementOptions);
			});
			case TYPE_VICTIM -> revoker.undoPunishmentByTypeAndPossibleVictims(type, victims).thenCompose((id) -> {
				if (id == null) {
					return revoker.futuresFactory().completedFuture(false);
				}
				return unenforceAndReturnTrue(id, type, enforcementOptions);
			});
		};
	}

	private CentralisedFuture<Boolean> unenforceAndReturnTrue(long id, PunishmentType type,
															  EnforcementOptions enforcementOptions) {
		return revoker.enforcement()
				.unenforce(id, type, (EnforcementOpts) enforcementOptions)
				.thenApply((ignore) -> true);
	}

	@Override
	public ReactionStage<Optional<Punishment>> undoAndGetPunishment(EnforcementOptions enforcementOptions) {
		return undoAndGetPunishmentWithoutUnenforcement().thenCompose((punishment) -> {
			if (punishment == null) {
				return CompletableFuture.completedStage(Optional.empty());
			}
			return punishment.unenforcePunishment(enforcementOptions).thenApply((ignore) -> Optional.of(punishment));
		});
	}

	private ReactionStage<Punishment> undoAndGetPunishmentWithoutUnenforcement() {
		return switch (getApproach()) {
			case ID -> revoker.undoAndGetPunishmentById(id);
			case ID_TYPE -> revoker.undoAndGetPunishmentByIdAndType(id, type);
			case TYPE_VICTIM -> revoker.undoAndGetPunishmentByTypeAndPossibleVictims(type, victims);
		};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RevocationOrderImpl that = (RevocationOrderImpl) o;
		return id == that.id && type == that.type && Objects.equals(victims, that.victims);
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (victims != null ? victims.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RevocationOrderImpl [id=" + id + ", type=" + type + ", victims=" + victims + "]";
	}

}
