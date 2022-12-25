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

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.jooq.OperatorBinding;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.libertybans.core.punish.Mode;

import java.io.IOException;
import java.util.Objects;

public final class PacketEnforceUnenforce implements SynchronizationPacket {

	final long id;
	final PunishmentType type;
	final Mode mode;
	final EnforcementOptions.Broadcasting broadcasting;
	final String targetArgument;
	final Operator unOperator;

	static final byte PACKET_ID = (byte) 1;
	private static final OperatorBinding operatorBinding = new OperatorBinding();

	PacketEnforceUnenforce(long id, PunishmentType type,
								  Mode mode, EnforcementOptions.Broadcasting broadcasting,
								  String targetArgument, Operator unOperator) {
		this.id = id;
		this.type = Objects.requireNonNull(type, "type");
		this.mode = Objects.requireNonNull(mode, "mode");
		this.broadcasting = Objects.requireNonNull(broadcasting, "broadcasting");
		this.targetArgument = targetArgument;
		this.unOperator = unOperator;
	}

	public PacketEnforceUnenforce(long id, PunishmentType type, Mode mode, EnforcementOpts enforcementOptions) {
		this(
				id,
				type,
				mode,
				enforcementOptions.broadcasting(),
				enforcementOptions.targetArgument().orElse(null),
				enforcementOptions.unOperator().orElse(null)
		);
	}

	public PacketEnforceUnenforce(Punishment punishment, Mode mode, EnforcementOpts enforcementOptions) {
		this(
				punishment.getIdentifier(),
				punishment.getType(),
				mode,
				enforcementOptions.broadcasting(),
				enforcementOptions.targetArgument().orElse(null),
				enforcementOptions.unOperator().orElse(null)
		);
	}

	@Override
	public byte packetId() {
		return PACKET_ID;
	}

	@Override
	public void writeTo(ProtocolOutputStream output) throws IOException {
		output.writeLong(id);
		output.writeByte(type.ordinal());
		output.writeBoolean(mode.toBoolean());
		output.writeByte(broadcasting.ordinal());
		output.writeNullableNonEmptyString(targetArgument);
		if (unOperator == null) {
			output.writeBoolean(false);
		} else {
			output.writeBoolean(true);
			output.writeUUID(operatorBinding.operatorToUuid(unOperator));
		}
	}

	static PacketEnforceUnenforce readFrom(ProtocolInputStream input) throws IOException {
		long id = input.readLong();
		PunishmentType type = PunishmentType.values()[input.readByte()];
		Mode mode = Mode.fromBoolean(input.readBoolean());
		EnforcementOpts.Broadcasting broadcasting = EnforcementOpts.Broadcasting.values()[input.readByte()];
		String targetArgument = input.readNullableNonEmptyString();
		Operator unOperator;
		if (input.readBoolean()) {
			unOperator = operatorBinding.uuidToOperator(input.readUUID());
		} else {
			unOperator = null;
		}
		return new PacketEnforceUnenforce(id, type, mode, broadcasting, targetArgument, unOperator);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PacketEnforceUnenforce message = (PacketEnforceUnenforce) o;
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
		return "PacketEnforceUnenforce{" +
				"id=" + id +
				", type=" + type +
				", mode=" + mode +
				", broadcasting=" + broadcasting +
				", targetArgument='" + targetArgument + '\'' +
				", unOperator=" + unOperator +
				'}';
	}
}
