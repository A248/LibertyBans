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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.core.punish.Mode;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SynchronizationProtocolTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private SynchronizationProtocol synchronizationProtocol;
	private long id;

	@BeforeEach
	public void setSynchronizationProtocol() {
		synchronizationProtocol = new SynchronizationProtocol(futuresFactory);
		id = ThreadLocalRandom.current().nextLong();
	}

	@ParameterizedTest
	@EnumSource(PunishmentType.class)
	public void enactPunishment(PunishmentType type) {
		communicateMessageSuccessfully(new PacketEnforceUnenforce(
				id, type, Mode.DO, EnforcementOptions.Broadcasting.NORMAL, "user1", null
		));
	}

	@ParameterizedTest
	@EnumSource(PunishmentType.class)
	public void undoPunishment(PunishmentType type) {
		communicateMessageSuccessfully(new PacketEnforceUnenforce(
				id, type, Mode.UNDO, EnforcementOptions.Broadcasting.NORMAL, "user2", PlayerOperator.of(UUID.randomUUID())
		));
	}

	@Test
	public void consoleOperator() {
		communicateMessageSuccessfully(new PacketEnforceUnenforce(
				id, PunishmentType.BAN, Mode.UNDO, EnforcementOptions.Broadcasting.NORMAL, "user3", ConsoleOperator.INSTANCE
		));
	}

	@ParameterizedTest
	@EnumSource(EnforcementOptions.Broadcasting.class)
	public void broadcasting(EnforcementOptions.Broadcasting broadcasting) {
		communicateMessageSuccessfully(new PacketEnforceUnenforce(
				id, PunishmentType.KICK, Mode.DO, broadcasting, "user4", PlayerOperator.of(UUID.randomUUID())
		));
	}

	@Test
	public void expunge() {
		communicateMessageSuccessfully(new PacketExpunge(id));
	}

	@Test
	public void updateDetails() {
		communicateMessageSuccessfully(new PacketUpdateDetails(id));
	}

	private void communicateMessageSuccessfully(SynchronizationPacket message) {
		byte[] serializedMessage = synchronizationProtocol.serializeMessage(message);

		synchronizationProtocol.receiveMessage(serializedMessage, receivedMessage -> {
			assertEquals(message, receivedMessage);
			return futuresFactory.completedFuture(null);
		}).toCompletableFuture().join();
	}

}
