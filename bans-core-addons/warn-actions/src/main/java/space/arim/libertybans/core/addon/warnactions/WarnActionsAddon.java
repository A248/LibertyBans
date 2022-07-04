/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.addon.warnactions;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.event.PostPunishEvent;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.events.ListenerPriorities;
import space.arim.omnibus.events.RegisteredListener;

@Singleton
public final class WarnActionsAddon extends AbstractAddon<WarnActionsConfig> {

	private final Omnibus omnibus;
	private final Provider<WarnActionsListener> listener;

	private RegisteredListener registeredListener;

	@Inject
	public WarnActionsAddon(AddonCenter addonCenter, Omnibus omnibus, Provider<WarnActionsListener> listener) {
		super(addonCenter);
		this.omnibus = omnibus;
		this.listener = listener;
	}

	@Override
	public void startup() {
		registeredListener = omnibus.getEventBus().registerListener(
				PostPunishEvent.class, ListenerPriorities.NORMAL, listener.get());
	}

	@Override
	public void shutdown() {
		omnibus.getEventBus().unregisterListener(registeredListener);
	}

	@Override
	public Class<WarnActionsConfig> configInterface() {
		return WarnActionsConfig.class;
	}

	@Override
	public String identifier() {
		return "warn-actions";
	}
}
