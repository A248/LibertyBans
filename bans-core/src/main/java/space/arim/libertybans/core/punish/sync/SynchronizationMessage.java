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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.jooq.OperatorBinding;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.libertybans.core.punish.Mode;
import space.arim.omnibus.util.ThisClass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SynchronizationMessage {

	final long id;
	final PunishmentType type;
	final Mode mode;
	final EnforcementOptions.Broadcasting broadcasting;
	final String targetArgument;
	final Operator unOperator;

	private static final byte PROTOCOL_VERSION = (byte) 1;
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	private static final OperatorBinding operatorBinding = new OperatorBinding();

	public SynchronizationMessage(long id, PunishmentType type,
								  Mode mode, EnforcementOptions.Broadcasting broadcasting,
								  String targetArgument, Operator unOperator) {
		this.id = id;
		this.type = Objects.requireNonNull(type, "type");
		this.mode = Objects.requireNonNull(mode, "mode");
		this.broadcasting = Objects.requireNonNull(broadcasting, "broadcasting");
		this.targetArgument = targetArgument;
		this.unOperator = unOperator;
	}

	public SynchronizationMessage(long id, PunishmentType type, Mode mode, EnforcementOpts enforcementOptions) {
		this(
				id,
				type,
				mode,
				enforcementOptions.broadcasting(),
				enforcementOptions.targetArgument().orElse(null),
				enforcementOptions.unOperator().orElse(null)
		);
	}

	public SynchronizationMessage(Punishment punishment, Mode mode, EnforcementOpts enforcementOptions) {
		this(
				punishment.getIdentifier(),
				punishment.getType(),
				mode,
				enforcementOptions.broadcasting(),
				enforcementOptions.targetArgument().orElse(null),
				enforcementOptions.unOperator().orElse(null)
		);
	}

	private static void writeUUID(DataOutputStream dataOutputStream, UUID uuid) throws IOException {
		dataOutputStream.writeLong(uuid.getMostSignificantBits());
		dataOutputStream.writeLong(uuid.getLeastSignificantBits());
	}

	private static UUID readUUID(DataInputStream dataInputStream) throws IOException {
		return new UUID(dataInputStream.readLong(), dataInputStream.readLong());
	}

	private static void writeNullableString(DataOutputStream dataOutputStream, String value) throws IOException {
		assert value == null || !value.isEmpty();
		dataOutputStream.writeUTF(value == null ? "" : value);
	}

	private static String readNullableString(DataInputStream dataInputStream) throws IOException {
		String value = dataInputStream.readUTF();
		return value.isEmpty() ? null : value;
	}

	void writeTo(UUID instanceId, DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(PROTOCOL_VERSION);
		writeUUID(dataOutputStream, instanceId);
		dataOutputStream.writeLong(id);
		dataOutputStream.writeByte(type.ordinal());
		dataOutputStream.writeBoolean(mode.toBoolean());
		dataOutputStream.writeByte(broadcasting.ordinal());
		writeNullableString(dataOutputStream, targetArgument);
		if (unOperator == null) {
			dataOutputStream.writeBoolean(false);
		} else {
			dataOutputStream.writeBoolean(true);
			writeUUID(
					dataOutputStream,
					operatorBinding.operatorToUuid(unOperator)
			);
		}
	}

	static Optional<SynchronizationMessage> readFrom(UUID instanceId, DataInputStream dataInputStream) throws IOException {

		byte protocolVersion = dataInputStream.readByte();
		if (protocolVersion != PROTOCOL_VERSION) {
			// We do not know how to handle this message
			logger.warn("Received message with newer protocol version: {}", protocolVersion);
			return Optional.empty();
		}
		if (instanceId.equals(readUUID(dataInputStream))) {
			// This is our own message
			logger.trace("Received own message");
			return Optional.empty();
		}
		long id = dataInputStream.readLong();
		PunishmentType type = PunishmentType.values()[dataInputStream.readByte()];
		Mode mode = Mode.fromBoolean(dataInputStream.readBoolean());
		EnforcementOpts.Broadcasting broadcasting = EnforcementOpts.Broadcasting.values()[dataInputStream.readByte()];
		String targetArgument = readNullableString(dataInputStream);
		Operator unOperator;
		if (dataInputStream.readBoolean()) {
			unOperator = operatorBinding.uuidToOperator(readUUID(dataInputStream));
		} else {
			unOperator = null;
		}

		long remainingBytes = dataInputStream.transferTo(OutputStream.nullOutputStream());
		if (remainingBytes != 0) {
			throw new IllegalStateException("Stream must be empty after all data has been read");
		}
		return Optional.of(new SynchronizationMessage(id, type, mode, broadcasting, targetArgument, unOperator));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SynchronizationMessage message = (SynchronizationMessage) o;
		return id == message.id
				&& type == message.type
				&& mode == message.mode
				&& broadcasting == message.broadcasting
				&& Objects.equals(targetArgument, message.targetArgument)
				&& Objects.equals(unOperator, message.unOperator);
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + type.hashCode();
		result = 31 * result + mode.hashCode();
		result = 31 * result + broadcasting.hashCode();
		result = 31 * result + (targetArgument != null ? targetArgument.hashCode() : 0);
		result = 31 * result + (unOperator != null ? unOperator.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Message{" +
				"id=" + id +
				", type=" + type +
				", mode=" + mode +
				", broadcasting=" + broadcasting +
				", targetArgument='" + targetArgument + '\'' +
				", unOperator=" + unOperator +
				'}';
	}
}
