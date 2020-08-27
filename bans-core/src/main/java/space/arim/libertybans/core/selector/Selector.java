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

import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentSelection;
import space.arim.libertybans.api.PunishmentSelector;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.LibertyBansCore;

public class Selector implements PunishmentSelector {

	final LibertyBansCore core;
	
	private final SelectionImpl selectionImpl;
	private final IDImpl idImpl;
	private final ApplicableImpl applicableImpl;
	private final OtherImpl otherImpl;
	
	public Selector(LibertyBansCore core) {
		this.core = core;

		selectionImpl = new SelectionImpl(this);
		idImpl = new IDImpl(this);
		applicableImpl = new ApplicableImpl(this);
		otherImpl = new OtherImpl(this);
	}
	
	/*
	 * 
	 * PunishmentSelection methods
	 * 
	 */
	
	@Override
	public CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection) {
		return selectionImpl.getFirstSpecificPunishment(selection);
	}
	
	@Override
	public CentralisedFuture<Set<Punishment>> getSpecificPunishments(PunishmentSelection selection) {
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
	 * @param address the player IP address
	 * @return a future which yields the ban itself, or null if there is none
	 */
	public CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, byte[] address) {
		return applicableImpl.executeAndCheckConnection(uuid, name, address);
	}
	
	@Override
	public CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, byte[] address, PunishmentType type) {
		return applicableImpl.getApplicablePunishment(uuid, address, type);
	}
	
	@Override
	public CentralisedFuture<Punishment> getCachedMute(UUID uuid, byte[] address) {
		Objects.requireNonNull(uuid, "uuid");
		return core.getMuteCacher().getCachedMute(uuid, address.clone());
	}
	
	CentralisedFuture<Punishment> getApplicableMute(UUID uuid, byte[] address) {
		return applicableImpl.getApplicableMute(uuid, address);
	}
	
	/*
	 * 
	 * Other methods
	 * 
	 */
	
	@Override
	public CentralisedFuture<Set<Punishment>> getHistoryForVictim(Victim victim) {
		return otherImpl.getHistoryForVictim(victim);
	}
	
	@Override
	public CentralisedFuture<Set<Punishment>> getActivePunishmentsForType(PunishmentType type) {
		return otherImpl.getActivePunishmentsForType(type);
	}
	
}
