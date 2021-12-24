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
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

@Singleton
public final class SynchronizationProtocol {

	private final FactoryOfTheFuture futuresFactory;

	/** Used to ensure we do not receive our own messages */
	private final UUID instanceId = UUID.randomUUID();

	@Inject
	public SynchronizationProtocol(FactoryOfTheFuture futuresFactory) {
		this.futuresFactory = futuresFactory;
	}

	public byte[] serializeMessage(SynchronizationMessage message) {
		try (ByteArrayOutputStream messageOutput = new ByteArrayOutputStream();
			 DataOutputStream dataOutputStream = new DataOutputStream(messageOutput)) {

			message.writeTo(instanceId, dataOutputStream);

			return messageOutput.toByteArray();
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to write synchronization message data", ex);
		}
	}

	public ReactionStage<?> receiveMessage(byte[] messageData, MessageReceiver messageReceiver) {
		Optional<SynchronizationMessage> optMessage;
		try (ByteArrayInputStream messageInput = new ByteArrayInputStream(messageData);
			 DataInputStream dataInputStream = new DataInputStream(messageInput)) {

			optMessage = SynchronizationMessage.readFrom(instanceId, dataInputStream);

		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to read synchronization message data", ex);
		}
		if (optMessage.isEmpty()) {
			return futuresFactory.completedFuture(null);
		}
		SynchronizationMessage message = optMessage.get();
		return messageReceiver.onReception(message);
	}

}
