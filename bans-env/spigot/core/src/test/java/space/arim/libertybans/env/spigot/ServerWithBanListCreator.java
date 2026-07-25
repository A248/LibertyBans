/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.env.spigot;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Server;

import java.util.Objects;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ServerWithBanListCreator {

	private final BanList userBans;

	private ServerWithBanListCreator(BanList userBans) {
		this.userBans = Objects.requireNonNull(userBans, "userBans");
	}

	private static BanList createBanList(Set<BanEntry> banEntries) {
		BanList banList = mock(BanList.class);
		when(banList.getBanEntries()).thenReturn(Set.copyOf(banEntries));
		return banList;
	}

	private static ServerWithBanListCreator userBans(BanList userBans) {
		return new ServerWithBanListCreator(userBans);
	}

	public static ServerWithBanListCreator userBans(Set<BanEntry> banEntries) {
		return userBans(createBanList(banEntries));
	}

	private Server ipBans(BanList ipBans) {
		Server server = mock(Server.class);
		when(server.getBanList(BanList.Type.NAME)).thenReturn(userBans);
		when(server.getBanList(BanList.Type.IP)).thenReturn(ipBans);
		return server;
	}

	public Server ipBans(Set<BanEntry> banEntries) {
		return ipBans(createBanList(banEntries));
	}

}
