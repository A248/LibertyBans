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
package space.arim.libertybans.core.selector;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.LibertyBansCore;

public class Selector implements PunishmentSelector {

	final LibertyBansCore core;
	
	private final SelectionImpl selectionImpl;
	private final IDImpl idImpl;
	private final ApplicableImpl applicableImpl;
	
	public Selector(LibertyBansCore core) {
		this.core = core;

		selectionImpl = new SelectionImpl(this);
		idImpl = new IDImpl(this);
		applicableImpl = new ApplicableImpl(this);
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
	
	CentralisedFuture<Punishment> getFirstSpecificPunishment(SelectionOrder selection) {
		return selectionImpl.getFirstSpecificPunishment(selection);
	}
	
	CentralisedFuture<Set<Punishment>> getSpecificPunishments(SelectionOrder selection) {
		return selectionImpl.getSpecificPunishments(selection);
	}
	
	/*
	 * 
	 * ID related methods
	 * 
	 */
	
	@Override
	public CentralisedFuture<Punishment> getActivePunishmentById(int id) {
		return idImpl.getActivePunishmentById(id);
	}
	
	@Override
	public CentralisedFuture<Punishment> getActivePunishmentByIdAndType(int id, PunishmentType type) {
		return idImpl.getActivePunishmentByIdAndType(id, type);
	}
	
	@Override
	public CentralisedFuture<Punishment> getHistoricalPunishmentById(int id) {
		return idImpl.getHistoricalPunishmentById(id);
	}
	
	/*
	 * 
	 * Applicability methods
	 * 
	 */
	
	/**
	 * Checks a player connection's in a single connection query, enforcing any applicable bans
	 * 
	 * @param uuid the player UUID
	 * @param name the player name
	 * @param address the player address
	 * @return a future which yields the ban itself, or null if there is none
	 */
	public CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address) {
		return applicableImpl.executeAndCheckConnection(uuid, name, address);
	}
	
	@Override
	public CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type) {
		return applicableImpl.getApplicablePunishment(uuid, address, type);
	}
	
	@Override
	public CentralisedFuture<Punishment> getCachedMute(UUID uuid, NetworkAddress address) {
		Objects.requireNonNull(uuid, "uuid");
		return core.getMuteCacher().getCachedMute(uuid, address);
	}
	
	CentralisedFuture<Punishment> getApplicableMute(UUID uuid, NetworkAddress address) {
		return applicableImpl.getApplicableMute(uuid, address);
	}
	
}
