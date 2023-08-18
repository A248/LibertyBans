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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class SelectorImpl implements InternalSelector {

	private final Configs configs;
	private final IDImpl idImpl;
	private final Gatekeeper gatekeeper;
	private final Provider<MuteCache> muteCache;
	private final SelectionResources resources;

	@Inject
	public SelectorImpl(Configs configs, IDImpl idImpl, Gatekeeper gatekeeper,
						Provider<MuteCache> muteCache, SelectionResources resources) {
		this.configs = configs;
		this.idImpl = idImpl;
		this.gatekeeper = gatekeeper;
		this.muteCache = muteCache;
		this.resources = resources;
	}

	@Override
	public SelectionOrderBuilder selectionBuilder() {
		return new SelectionOrderBuilderImpl(resources);
	}

	@Override
	public SelectionOrderBuilder selectionBuilder(SelectionResources resources) {
		return new SelectionOrderBuilderImpl(resources);
	}

	@Override
	public SelectionByApplicabilityBuilderImpl selectionByApplicabilityBuilder(UUID uuid, NetworkAddress address) {
		AddressStrictness strictness = configs.getMainConfig().enforcement().addressStrictness();
		return new SelectionByApplicabilityBuilderImpl(resources, uuid, address, strictness);
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
	public CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address,
																  Set<ServerScope> scopes) {
		return gatekeeper.executeAndCheckConnection(uuid, name, address, scopes, this);
	}

	@Override
	public ReactionStage<Optional<Punishment>> getCachedMute(UUID uuid, NetworkAddress address) {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(address, "address");
		return muteCache.get().getCachedMute(uuid, address);
	}

}
