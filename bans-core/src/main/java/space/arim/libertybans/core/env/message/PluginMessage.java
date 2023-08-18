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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public interface PluginMessage<D, R> {

	String subchannelName();

	void writeData(D data, PluginMessageOutput output) throws IOException;

	R readResponse(PluginMessageInput input) throws IOException;

	default void writeTo(D data, PluginMessageOutput output) {
		try {
			output.writeUTF(subchannelName());
			writeData(data, output);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	default Optional<R> readFrom(PluginMessageInput input) {
		try {
			String subchannel = input.readUTF();
			if (!subchannel.equals(subchannelName())) {
				return Optional.empty();
			}
			return Optional.of(readResponse(input));
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}

	}

}
