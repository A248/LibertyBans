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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.core.selector.MuteCacher;

class RevocationOrderImpl implements RevocationOrder {
	
	private transient final EnforcementCenter center;

	private final int id;
	private final PunishmentType type;
	private final Victim victim;
	
	private RevocationOrderImpl(EnforcementCenter center, int id, PunishmentType type, Victim victim) {
		this.center = center;

		this.id = id;
		this.type = type;
		this.victim = victim;
	}
	
	RevocationOrderImpl(EnforcementCenter center, int id) {
		this(center, id, null, null);
		assert getApproach() == Approach.ID;
	}
	
	RevocationOrderImpl(EnforcementCenter center, int id, PunishmentType type) {
		this(center, id, Objects.requireNonNull(type, "type"), null);
		assert getApproach() == Approach.ID_TYPE;
	}
	
	RevocationOrderImpl(EnforcementCenter center, PunishmentType type, Victim victim) {
		this(center, -1, MiscUtil.checkSingular(type), Objects.requireNonNull(victim, "victim"));
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
	public Integer getID() {
		return id;
	}

	@Override
	public PunishmentType getType() {
		return type;
	}

	@Override
	public Victim getVictim() {
		return victim;
	}

	@Override
	public CentralisedFuture<Boolean> undoPunishment() {
		return undoPunishmentWithoutUnenforcement().thenApply((revoked) -> {
			if (revoked) {
				MuteCacher muteCacher = center.core().getMuteCacher();
				switch (getApproach()) {
				case ID:
					muteCacher.clearCachedMute(id);
					break;
				case ID_TYPE:
					if (type == PunishmentType.MUTE) {
						muteCacher.clearCachedMute(id);
					}
					break;
				case TYPE_VICTIM:
					if (type == PunishmentType.MUTE) {
						muteCacher.clearCachedMute(victim);
					}
					break;
				default:
					throw MiscUtil.unknownEnumEntry(getApproach());
				}
			}
			return revoked;
		});
	}

	@Override
	public CentralisedFuture<Boolean> undoPunishmentWithoutUnenforcement() {
		Revoker revoker = center.getRevoker();
		switch (getApproach()) {
		case ID:
			return revoker.undoPunishmentById(id);
		case ID_TYPE:
			return revoker.undoPunishmentByIdAndType(id, type);
		case TYPE_VICTIM:
			return revoker.undoPunishmentByTypeAndVictim(type, victim);
		default:
			throw MiscUtil.unknownEnumEntry(getApproach());
		}
	}
	
	@Override
	public CentralisedFuture<Punishment> undoAndGetPunishment() {
		return undoAndGetPunishmentWithoutUnenforcement().thenCompose((punishment) -> {
			if (punishment == null) {
				return CompletableFuture.completedStage(null);
			}
			return punishment.unenforcePunishment().thenApply((ignore) -> punishment);
		});
	}

	@Override
	public CentralisedFuture<Punishment> undoAndGetPunishmentWithoutUnenforcement() {
		Revoker revoker = center.getRevoker();
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + Objects.hashCode(type);
		result = prime * result + Objects.hashCode(victim);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof RevocationOrderImpl)) {
			return false;
		}
		RevocationOrderImpl other = (RevocationOrderImpl) object;
		return id == other.id
				&& type == other.type
				&& Objects.equals(victim, other.victim);
	}

	@Override
	public String toString() {
		return "RevocationOrderImpl [id=" + id + ", type=" + type + ", victim=" + victim + "]";
	}
	
}
