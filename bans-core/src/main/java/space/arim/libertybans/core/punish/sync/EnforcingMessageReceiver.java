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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.libertybans.core.punish.LocalEnforcer;
import space.arim.libertybans.core.punish.Mode;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

public final class EnforcingMessageReceiver implements MessageReceiver {

	private final FactoryOfTheFuture futuresFactory;
	private final PunishmentSelector selector;
	private final LocalEnforcer enforcer;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public EnforcingMessageReceiver(FactoryOfTheFuture futuresFactory, PunishmentSelector selector, LocalEnforcer enforcer) {
		this.futuresFactory = futuresFactory;
		this.selector = selector;
		this.enforcer = enforcer;
	}

	@Override
	public ReactionStage<?> onReception(SynchronizationPacket message) {
		if (message instanceof PacketEnforceUnenforce packetEnforceUnenforce) {
			return onReception(packetEnforceUnenforce);
		} else if (message instanceof PacketExpunge packetExpunge) {
			return enforcer.clearExpungedWithoutSynchronization(packetExpunge.id);
		} else if (message instanceof PacketUpdateDetails packetUpdateDetails) {
			return enforcer.updateDetailsWithoutSynchronization(packetUpdateDetails.id);
		} else {
			logger.warn("Unknown packet {} ({})", message, message.getClass());
			return futuresFactory.completedFuture(null);
		}
	}

	private ReactionStage<?> onReception(PacketEnforceUnenforce message) {
		EnforcementOpts enforcementOptions = EnforcementOpts
				.builder()
				.enforcement(EnforcementOptions.Enforcement.SINGLE_SERVER_ONLY)
				.broadcasting(message.broadcasting)
				.targetArgument(message.targetArgument)
				.unOperator(message.unOperator)
				.build();
		if (message.broadcasting == EnforcementOptions.Broadcasting.NONE && message.mode == Mode.UNDO) {
			// Optimization: We do not need the full punishment details to simply undo a punishment
			return enforcer.unenforceWithoutSynchronization(message.id, message.type, enforcementOptions);
		}
		return selector.getHistoricalPunishmentByIdAndType(message.id, message.type).thenCompose((optPunishment) -> {
			if (optPunishment.isEmpty()) {
				logger.warn("Received punishment which does not exist: id {} and type {}", message.id, message.type);
				return futuresFactory.completedFuture(null);
			}
			Punishment punishment = optPunishment.get();
			if (message.mode == Mode.UNDO) {
				// Unenforce this punishment
				return enforcer.unenforceWithoutSynchronization(punishment, enforcementOptions);
			}
			// Enforce this punishment
			assert message.mode == Mode.DO : "Mode " + message.mode;
			return enforcer.enforceWithoutSynchronization(punishment, enforcementOptions);
		});
	}

}
