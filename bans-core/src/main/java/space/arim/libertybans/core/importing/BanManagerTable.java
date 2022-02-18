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

package space.arim.libertybans.core.importing;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.omnibus.util.UUIDUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public enum BanManagerTable {
	// Bans
	PLAYER_BANS(PunishmentType.BAN, VictimKind.uuid(), true),
	PLAYER_BAN_RECORDS(PunishmentType.BAN, VictimKind.uuid(), false),
	IP_BANS(PunishmentType.BAN, VictimKind.ipAddress(), true),
	IP_BAN_RECORDS(PunishmentType.BAN, VictimKind.ipAddress(), false),
	// Mutes
	PLAYER_MUTES(PunishmentType.MUTE, VictimKind.uuid(), true),
	PLAYER_MUTE_RECORDS(PunishmentType.MUTE, VictimKind.uuid(), false),
	IP_MUTES(PunishmentType.MUTE, VictimKind.ipAddress(), true),
	IP_MUTE_RECORDS(PunishmentType.MUTE, VictimKind.ipAddress(), false),
	// Warns
	PLAYER_WARNINGS(PunishmentType.WARN, VictimKind.uuid(), true),
	// Kicks
	PLAYER_KICKS(PunishmentType.KICK, VictimKind.uuid(), true)
	;

	final PunishmentType type;
	final VictimKind victimKind;
	/**
	 * Whether the punishments in this table are active. True for kicks
	 */
	public final boolean active;

	private BanManagerTable(PunishmentType type, VictimKind victimKind, boolean active) {
		this.type = type;
		this.victimKind = victimKind;
		this.active = active;
	}

	public String tableName(String tablePrefix) {
		return tablePrefix + name().toLowerCase(Locale.ROOT);
	}

	interface VictimKind {

		Victim mapVictim(ResultSet resultSet) throws SQLException;

		static VictimKind uuid() {
			return resultSet -> PlayerVictim.of(UUIDUtil.fromByteArray(resultSet.getBytes("player_id")));
		}

		static VictimKind ipAddress() {
			return resultSet -> AddressVictim.of(resultSet.getBytes("ip"));
		}
	}

}
