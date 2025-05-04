/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.env.sponge.plugin.PlatformAccess;
import space.arim.omnibus.util.ThisClass;

import java.util.Set;

public final class SpongeEnv implements Environment {

	private final Provider<ConnectionListener> connectionListener;
	private final Provider<JoinListener> joinListener;
	private final Provider<ChatListener> chatListener;
	private final PlatformAccess platformAccess;

	private static final Logger LOGGER = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public SpongeEnv(Provider<ConnectionListener> connectionListener,Provider<JoinListener> joinListener,
					 Provider<ChatListener> chatListener, PlatformAccess platformAccess) {
		this.connectionListener = connectionListener;
		this.joinListener = joinListener;
		this.chatListener = chatListener;
		this.platformAccess = platformAccess;
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of(
				connectionListener.get(), joinListener.get(), chatListener.get()
		);
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		LOGGER.debug("Skipping alias {} since the Sponge API does not support command unregistration.", command);
		class DummyListener implements PlatformListener {

			@Override
			public void register() {}

			@Override
			public void unregister() {}
		}
		return new DummyListener();
	}

	@Override
	public Object platformAccess() {
		return platformAccess;
	}

}
