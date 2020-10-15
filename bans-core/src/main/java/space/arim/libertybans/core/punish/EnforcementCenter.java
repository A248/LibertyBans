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

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.LibertyBansCore;

public class EnforcementCenter implements PunishmentDrafter {

	private final LibertyBansCore core;
	
	private final Enactor enactor;
	private final Enforcer enforcer;
	private final Revoker revoker;
	
	public EnforcementCenter(LibertyBansCore core) {
		this.core = core;
		enactor = new Enactor(this);
		enforcer = new Enforcer(this);
		revoker = new Revoker(this);
	}
	
	public Punishment createPunishment(int id, PunishmentType type, Victim victim, Operator operator, String reason,
			ServerScope scope, long start, long end) {
		Instant startDate = Instant.ofEpochSecond(start);
		Instant endDate = (end == 0L) ? Instant.MAX : Instant.ofEpochSecond(end);
		return new SecurePunishment(this, id, type, victim, operator, reason, scope, startDate, endDate);
	}
	
	LibertyBansCore core() {
		return core;
	}
	
	Enactor getEnactor() {
		return enactor;
	}
	
	Enforcer getEnforcer() {
		return enforcer;
	}
	
	public Revoker getRevoker() {
		return revoker;
	}
	
	/**
	 * Enforces an incoming connection, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * Adds the UUID and name to the local fast cache, queries for an applicable ban, and formats the
	 * ban reason as the punishment message.
	 * 
	 * @param uuid the player's UUID
	 * @param name the player's name
	 * @param address the player's network address
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	public CentralisedFuture<SendableMessage> executeAndCheckConnection(UUID uuid, String name, InetAddress address) {
		return getEnforcer().executeAndCheckConnection(uuid, name, NetworkAddress.of(address));
	}
	
	/**
	 * Enforces a chat message or executed command, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * If this corresponds to an executed command, the configured commands whose access to muted players to block
	 * are taken into account.
	 * 
	 * @param uuid the player's UUID
	 * @param address the player's network address
	 * @param command the command executed, or {@code null} if this is a chat message
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	public CentralisedFuture<SendableMessage> checkChat(UUID uuid, InetAddress address, String command) {
		return getEnforcer().checkChat(uuid, NetworkAddress.of(address), command);
	}

	@Override
	public DraftPunishmentBuilder draftBuilder() {
		return new DraftPunishmentBuilderImpl(this);
	}
	
}
