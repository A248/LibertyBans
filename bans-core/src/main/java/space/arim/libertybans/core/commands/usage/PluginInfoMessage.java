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

package space.arim.libertybans.core.commands.usage;

import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.config.ReadFromResource;
import space.arim.libertybans.core.env.CmdSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PluginInfoMessage {

	private final List<String> maintainers;
	private final List<String> collaborators;
	private final List<String> commendedUsers;

	PluginInfoMessage(List<String> maintainers, List<String> collaborators, List<String> commendedUsers) {
		this.maintainers = List.copyOf(maintainers);
		this.collaborators = List.copyOf(collaborators);
		this.commendedUsers = List.copyOf(commendedUsers);
	}

	public PluginInfoMessage() {
		this(List.of(), List.of(), List.of());
	}

	public static PluginInfoMessage fromReader(Reader reader) throws IOException {
		List<String> maintainers = new ArrayList<>();
		List<String> collaborators = new ArrayList<>();
		List<String> commendedUsers = new ArrayList<>();

		Deque<List<String>> contributors = new ArrayDeque<>(List.of(maintainers, collaborators, commendedUsers));

		try (BufferedReader bufRead = new BufferedReader(reader)) {
			String line;
			while ((line = bufRead.readLine()) != null) {
				if (line.isBlank()) {
					contributors.poll(); // Move to next list
				} else {
					contributors.getFirst().add(line); // Add to current list
				}
			}
		}
		return new PluginInfoMessage(maintainers, collaborators, commendedUsers);
	}

	public static PluginInfoMessage fromResource(String resourceName) {
		return new ReadFromResource(resourceName).read(PluginInfoMessage::fromReader);
	}

	public void send(CmdSender sender) {
		String message =
				"&9----------------------------------------------------------\n" +
				"&d&l" + PluginInfo.NAME + " &r&e&l" +  PluginInfo.VERSION + "&r\n" +
				"&7" + PluginInfo.DESCRIPTION + "\n" +
				"&7" + PluginInfo.URL + "\n" +
				"&9-- Contributors --\n" +
				"&7Maintainers: " + maintainers + "\n" +
				"&7Collaborators: " + collaborators + "\n" +
				"&7Commended Users: " + commendedUsers + "\n" +
				"&9----------------------------------------------------------\n";
		sender.sendLiteralMessageNoPrefix(message);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PluginInfoMessage that = (PluginInfoMessage) o;
		return maintainers.equals(that.maintainers) && collaborators.equals(that.collaborators) && commendedUsers.equals(that.commendedUsers);
	}

	@Override
	public int hashCode() {
		int result = maintainers.hashCode();
		result = 31 * result + collaborators.hashCode();
		result = 31 * result + commendedUsers.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "PluginInfoMessage{" +
				"maintainers=" + maintainers +
				", collaborators=" + collaborators +
				", commendedUsers=" + commendedUsers +
				'}';
	}
}
