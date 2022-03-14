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

package space.arim.libertybans.core.selector;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.SelectionOrderBuilder;

@Singleton
public class SelectorImpl implements InternalSelector {

	private final SelectionImpl selectionImpl;
	private final IDImpl idImpl;
	private final ApplicableImpl applicableImpl;
	private final Gatekeeper gatekeeper;
	private final Provider<MuteCache> muteCache;

	@Inject
	public SelectorImpl(SelectionImpl selectionImpl, IDImpl idImpl, ApplicableImpl applicableImpl,
						Gatekeeper gatekeeper, Provider<MuteCache> muteCache) {
		this.selectionImpl = selectionImpl;
		this.idImpl = idImpl;
		this.applicableImpl = applicableImpl;
		this.gatekeeper = gatekeeper;
		this.muteCache = muteCache;
	}

	@Override
	public SelectionOrderBuilder selectionBuilder() {
		return new SelectionOrderBuilderImpl(this);
	}

	/*
	 * 
	 * PunishmentSelection methods
	 * 
	 */

	ReactionStage<Punishment> getFirstSpecificPunishment(SelectionOrder selection) {
		return selectionImpl.getFirstSpecificPunishment(selection);
	}

	ReactionStage<List<Punishment>> getSpecificPunishments(SelectionOrder selection) {
		return selectionImpl.getSpecificPunishments(selection);
	}

	/*
	 * 
	 * ID related methods
	 * 
	 */

	@Override
	public ReactionStage<Optional<Punishment>> getActivePunishmentById(long id) {
		return idImpl.getActivePunishmentById(id).thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<Optional<Punishment>> getActivePunishmentByIdAndType(long id, PunishmentType type) {
		return idImpl.getActivePunishmentByIdAndType(id, type).thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<Optional<Punishment>> getHistoricalPunishmentById(long id) {
		return idImpl.getHistoricalPunishmentById(id).thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<Optional<Punishment>> getHistoricalPunishmentByIdAndType(long id, PunishmentType type) {
		return idImpl.getHistoricalPunishmentByIdAndType(id, type).thenApply(Optional::ofNullable);
	}

	/*
	 * 
	 * Applicability methods
	 * 
	 */

	@Override
	public CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address) {
		return gatekeeper.executeAndCheckConnection(uuid, name, address);
	}

	@Override
	public ReactionStage<Optional<Punishment>> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type) {
		return applicableImpl.getApplicablePunishment(uuid, address, type).thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<Optional<Punishment>> getCachedMute(UUID uuid, NetworkAddress address) {
		Objects.requireNonNull(uuid, "uuid");
		return muteCache.get().getCachedMute(uuid, address);
	}

	@Override
	public CentralisedFuture<Punishment> getApplicableMute(UUID uuid, NetworkAddress address) {
		return applicableImpl.getApplicablePunishment(uuid, address, PunishmentType.MUTE);
	}

}
