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

package space.arim.libertybans.core.punish.sync;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

@Singleton
public final class SynchronizationProtocol {

	private final FactoryOfTheFuture futuresFactory;

	/** Used to ensure we do not receive our own messages */
	private final UUID instanceId = UUID.randomUUID();

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public SynchronizationProtocol(FactoryOfTheFuture futuresFactory) {
		this.futuresFactory = futuresFactory;
	}

	public byte[] serializeMessage(SynchronizationPacket message) {
		try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			 ProtocolOutputStream output = new ProtocolOutputStream(byteOutput)) {

			output.writeByte(message.packetId());
			output.writeUUID(instanceId);
			message.writeTo(output);

			return byteOutput.toByteArray();
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to write synchronization packet data", ex);
		}
	}

	public ReactionStage<?> receiveMessage(byte[] messageData, MessageReceiver messageReceiver) {
		try (ByteArrayInputStream byteInput = new ByteArrayInputStream(messageData);
			 ProtocolInputStream input = new ProtocolInputStream(byteInput)) {

			byte packetId = input.readByte();
			if (instanceId.equals(input.readUUID())) {
				// This is our own message
				logger.trace("Received own message");
				return futuresFactory.completedFuture(null);
			}
			SynchronizationPacket message = switch (packetId) {
				case PacketEnforceUnenforce.PACKET_ID -> PacketEnforceUnenforce.readFrom(input);
				case PacketExpunge.PACKET_ID -> PacketExpunge.readFrom(input);
				case PacketUpdateDetails.PACKET_ID -> PacketUpdateDetails.readFrom(input);
				default -> null; // Exit below
			};
			if (message == null) {
				logger.warn("Unknown synchronization packet ID: {}", packetId);
				return futuresFactory.completedFuture(null);
			}
			long remainingBytes = input.transferTo(OutputStream.nullOutputStream());
			if (remainingBytes != 0) {
				throw new IllegalStateException("Stream must be empty after all data has been read");
			}
			return messageReceiver.onReception(message);

		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to read synchronization packet data", ex);
		}
	}

}
