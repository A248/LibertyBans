/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.env;

import java.util.ArrayList;
import java.util.List;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;

public class EnvironmentManager implements Part {

	private final LibertyBansCore core;
	
	private final List<PlatformListener> listeners = new ArrayList<>();
	
	public EnvironmentManager(LibertyBansCore core) {
		this.core = core;
	}

	@Override
	public void startup() {
		listeners.addAll(core.getEnvironment().createListeners());
		for (String alias : core.getMainConfig().commandAliases()) {
			listeners.add(core.getEnvironment().createAliasCommand(alias));
		}
		listeners.forEach(PlatformListener::register);
	}

	@Override
	public void shutdown() {
		listeners.forEach(PlatformListener::unregister);
	}
	
}
