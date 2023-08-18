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

package space.arim.libertybans.core.env.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;

public final class KickPlayer implements PluginMessage<KickPlayer.Data, Void> {

	@Override
	public String subchannelName() {
		return "KickPlayer";
	}

	@Override
	public void writeData(Data data, PluginMessageOutput output) throws IOException {
		output.writeUTF(data.playerName);
		output.writeUTF(LegacyComponentSerializer.legacySection().serialize(data.message));
	}

	@Override
	public Void readResponse(PluginMessageInput input) throws IOException {
		return null;
	}

	public record Data(String playerName, Component message) {}

}
