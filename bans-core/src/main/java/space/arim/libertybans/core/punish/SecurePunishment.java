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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.PunishmentEditor;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;

class SecurePunishment extends AbstractPunishmentBase implements Punishment, EnforcementOpts.Factory {

	private final SecurePunishmentCreator creator;
	private final long id;
	private final Instant startDate;
	private final Instant endDate;

	SecurePunishment(SecurePunishmentCreator creator,
					 long id, PunishmentType type, Victim victim, Operator operator, String reason,
					 ServerScope scope, Instant startDate, Instant endDate, EscalationTrack escalationTrack) {
		super(type, victim, operator, reason, scope, escalationTrack);
		this.creator = Objects.requireNonNull(creator, "creator");
		this.id = id;
		this.startDate = Objects.requireNonNull(startDate, "startDate");
		this.endDate = Objects.requireNonNull(endDate, "endDate");
	}

	@Override
	public long getIdentifier() {
		return id;
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}
	
	@Override
	public Instant getEndDate() {
		return endDate;
	}

	@Override
	public CentralisedFuture<Void> enforcePunishment(EnforcementOptions enforcementOptions) {
		return creator.enforcement().enforce(this, (EnforcementOpts) enforcementOptions);
	}

	@Override
	public ReactionStage<Boolean> undoPunishment(EnforcementOptions enforcementOptions) {
		return creator.revoker().undoPunishment(this).thenCompose((undone) -> {
			if (!undone) {
				return CompletableFuture.completedFuture(false);
			}
			return unenforcePunishment(enforcementOptions).thenApply((ignore) -> true);
		});
	}

	@Override
	public ReactionStage<?> unenforcePunishment(EnforcementOptions enforcementOptions) {
		return creator.enforcement().unenforce(this, (EnforcementOpts) enforcementOptions);
	}

	@Override
	public ReactionStage<Optional<Punishment>> modifyPunishment(Consumer<PunishmentEditor> editorConsumer) {
		Modifier.Editor editor = creator.modifer().new Editor(this);
		editorConsumer.accept(editor);
		return editor.modify();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SecurePunishment that = (SecurePunishment) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return (int) (31 + id);
	}

	@Override
	public String toString() {
		return "SecurePunishment [id=" + id + ", getType()=" + getType() + ", getVictim()=" + getVictim()
				+ ", getOperator()=" + getOperator() + ", getReason()=" + getReason() + ", getScope()=" + getScope()
				+ ", getStartDateSeconds()=" + getStartDateSeconds() + ", getEndDateSeconds()=" + getEndDateSeconds()
				+ ", getEscalationTrack()=" + getEscalationTrack()
				+ "]";
	}

}
