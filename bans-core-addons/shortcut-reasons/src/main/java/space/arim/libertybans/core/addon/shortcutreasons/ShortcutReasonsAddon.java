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

package space.arim.libertybans.core.addon.shortcutreasons;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.omnibus.Omnibus;

@Singleton
public final class ShortcutReasonsAddon extends AbstractAddon<ShortcutReasonsConfig> {

	private final Omnibus omnibus;
	private final Provider<ShortcutReasonsListener> listener;

	@Inject
	public ShortcutReasonsAddon(AddonCenter addonCenter, Omnibus omnibus, Provider<ShortcutReasonsListener> listener) {
		super(addonCenter);
		this.omnibus = omnibus;
		this.listener = listener;
	}

	@Override
	public void startup() {
		omnibus.getEventBus().registerListeningMethods(listener.get());
	}

	@Override
	public void shutdown() {
		omnibus.getEventBus().unregisterListeningMethods(listener.get());
	}

	@Override
	public Class<ShortcutReasonsConfig> configInterface() {
		return ShortcutReasonsConfig.class;
	}

	@Override
	public String identifier() {
		return "shortcut-reasons";
	}

}
