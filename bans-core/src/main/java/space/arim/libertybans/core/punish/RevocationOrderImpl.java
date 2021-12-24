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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class RevocationOrderImpl implements RevocationOrder, EnforcementOpts.Factory {
	
	private final Revoker revoker;
	private final long id;
	private final PunishmentType type;
	private final Victim victim;

	private RevocationOrderImpl(Revoker revoker, long id, PunishmentType type, Victim victim) {
		this.revoker = revoker;
		this.id = id;
		this.type = type;
		this.victim = victim;
	}

	RevocationOrderImpl(Revoker revoker, long id) {
		this(revoker, id, null, null);
		assert getApproach() == Approach.ID;
	}

	RevocationOrderImpl(Revoker revoker, long id, PunishmentType type) {
		this(revoker, id, Objects.requireNonNull(type, "type"), null);
		assert getApproach() == Approach.ID_TYPE;
	}

	RevocationOrderImpl(Revoker revoker, PunishmentType type, Victim victim) {
		this(revoker, -1, MiscUtil.checkSingular(type), Objects.requireNonNull(victim, "victim"));
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
		if (victim != null) {
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
	public Optional<Victim> getVictim() {
		return Optional.ofNullable(victim);
	}

	@Override
	public ReactionStage<Boolean> undoPunishment(EnforcementOptions enforcementOptions) {
		// Unenforcement needs both an ID and a type
		// Revoking by ID needs to be special-cased so that unenforcement can use the punishment type
		switch (getApproach()) {
		case ID:
			return revoker.undoPunishmentById(id).thenCompose((type) -> {
				if (type == null) {
					return revoker.futuresFactory().completedFuture(false);
				}
				return unenforceAndReturnTrue(id, type, enforcementOptions);
			});
		case ID_TYPE:
			return revoker.undoPunishmentByIdAndType(id, type).thenCompose((revoked) -> {
				if (!revoked) {
					return revoker.futuresFactory().completedFuture(false);
				}
				return unenforceAndReturnTrue(id, type, enforcementOptions);
			});
		case TYPE_VICTIM:
			return revoker.undoPunishmentByTypeAndVictim(type, victim).thenCompose((id) -> {
				if (id == null) {
					return revoker.futuresFactory().completedFuture(false);
				}
				return unenforceAndReturnTrue(id, type, enforcementOptions);
			});
		default:
			throw MiscUtil.unknownEnumEntry(getApproach());
		}
	}

	private CentralisedFuture<Boolean> unenforceAndReturnTrue(long id, PunishmentType type,
															  EnforcementOptions enforcementOptions) {
		return revoker.enforcement().unenforce(id, type, (EnforcementOpts) enforcementOptions)
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
		switch (getApproach()) {
		case ID:
			return revoker.undoAndGetPunishmentById(id);
		case ID_TYPE:
			return revoker.undoAndGetPunishmentByIdAndType(id, type);
		case TYPE_VICTIM:
			return revoker.undoAndGetPunishmentByTypeAndVictim(type, victim);
		default:
			throw MiscUtil.unknownEnumEntry(getApproach());
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RevocationOrderImpl that = (RevocationOrderImpl) o;
		return id == that.id && type == that.type && Objects.equals(victim, that.victim);
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (victim != null ? victim.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RevocationOrderImpl [id=" + id + ", type=" + type + ", victim=" + victim + "]";
	}
	
}
