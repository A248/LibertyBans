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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.punish.sync.MessageReceiver;
import space.arim.libertybans.core.punish.sync.SynchronizationMessage;
import space.arim.libertybans.core.punish.sync.SynchronizationMessenger;
import space.arim.libertybans.core.punish.sync.SynchronizationProtocol;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.function.Supplier;

@Singleton
public final class StandardGlobalEnforcement implements GlobalEnforcement {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final LocalEnforcer enforcer;
	private final SynchronizationProtocol synchronizationProtocol;
	private final Provider<SynchronizationMessenger> synchronizationMessenger;
	private final MessageReceiver messageReceiver;

	@Inject
	public StandardGlobalEnforcement(Configs configs, FactoryOfTheFuture futuresFactory, LocalEnforcer enforcer,
									 SynchronizationProtocol synchronizationProtocol,
									 Provider<SynchronizationMessenger> synchronizationMessenger,
									 MessageReceiver messageReceiver) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.synchronizationProtocol = synchronizationProtocol;
		this.enforcer = enforcer;
		this.synchronizationMessenger = synchronizationMessenger;
		this.messageReceiver = messageReceiver;
	}

	// Dispatch

	@Override
	public CentralisedFuture<?> enforce(Punishment punishment, EnforcementOpts enforcementOptions) {
		return handleSynchronizedEnforcement(
				() -> enforcer.enforceWithoutSynchronization(punishment, enforcementOptions),
				enforcementOptions,
				new SynchronizationMessage(punishment, Mode.DO, enforcementOptions)
		);
	}

	// Comes from Punishment#unenforce
	@Override
	public CentralisedFuture<?> unenforce(Punishment punishment, EnforcementOpts enforcementOptions) {
		return handleSynchronizedEnforcement(
				() -> enforcer.unenforceWithoutSynchronization(punishment, enforcementOptions),
				enforcementOptions,
				new SynchronizationMessage(punishment, Mode.UNDO, enforcementOptions)
		);
	}

	// Comes from RevocationOrderImpl#undoPunishment
	@Override
	public CentralisedFuture<?> unenforce(long id, PunishmentType type, EnforcementOpts enforcementOptions) {
		return handleSynchronizedEnforcement(
				() -> enforcer.unenforceWithoutSynchronization(id, type, enforcementOptions),
				enforcementOptions,
				new SynchronizationMessage(id, type, Mode.UNDO, enforcementOptions)
		);
	}

	private CentralisedFuture<?> handleSynchronizedEnforcement(Supplier<CentralisedFuture<?>> localEnforcement,
															   EnforcementOptions enforcementOptions,
															   SynchronizationMessage message) {
		EnforcementOptions.Enforcement enforcement = enforcementOptions.enforcement();
		switch (enforcement) {
		case GLOBAL:
			if (configs.getSqlConfig().synchronization().enabled()) {
				// Need to dispatch message to other instances
				return localEnforcement.get().thenCompose((ignore) -> {
					return synchronizationMessenger.get().dispatch(synchronizationProtocol.serializeMessage(message));
				});
			}
			return localEnforcement.get();
		case SINGLE_SERVER_ONLY:
			return localEnforcement.get();
		case NONE:
			return futuresFactory.completedFuture(null);
		default:
			throw MiscUtil.unknownEnumEntry(enforcement);
		}
	}

	// Reception

	@Override
	public void run() {
		synchronizationMessenger.get().poll().thenCompose(this::receiveAllMessages).join();
	}

	ReactionStage<?> receiveAllMessages(byte[][] messages) {
		// Receive the messages in order
		ReactionStage<?> future = null;
		for (int n = 0; n < messages.length; n++) {
			if (future == null) {
				future = receiveMessage(messages[n]);
			} else {
				int finalIndex = n;
				future = future.thenCompose((ignore) -> receiveMessage(messages[finalIndex]));
			}
		}
		if (future == null) {
			// Empty
			return futuresFactory.completedFuture(null);
		}
		return future;
	}

	private ReactionStage<?> receiveMessage(byte[] message) {
		return synchronizationProtocol.receiveMessage(message, messageReceiver);
	}
}
