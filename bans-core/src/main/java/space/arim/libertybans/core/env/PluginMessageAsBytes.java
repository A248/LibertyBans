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

package space.arim.libertybans.core.env;

import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.libertybans.core.env.message.PluginMessageInput;
import space.arim.libertybans.core.env.message.PluginMessageOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public record PluginMessageAsBytes<D, R>(PluginMessage<D, R> pluginMessage) {

	public byte[] generateBytes(D data) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			 DataOutputStream dataOutput = new DataOutputStream(outputStream)) {

			pluginMessage.writeTo(data, new DataOutputAsOutput(dataOutput));
			return outputStream.toByteArray();

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public Optional<R> readBytes(byte[] data) {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
			 DataInputStream dataInput = new DataInputStream(inputStream)) {

			return pluginMessage.readFrom(new DataInputAsInput(dataInput));
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	record DataOutputAsOutput(DataOutput dataOutput) implements PluginMessageOutput {
		@Override
		public void writeUTF(String utf) throws IOException {
			dataOutput.writeUTF(utf);
		}
	}

	record DataInputAsInput(DataInput dataInput) implements PluginMessageInput {
		@Override
		public String readUTF() throws IOException {
			return dataInput.readUTF();
		}
	}
}
