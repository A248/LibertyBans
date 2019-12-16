/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.events.bukkit;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.events.UniversalUnpunishEvent;

public class PostUnpunishEvent extends AbstractBukkitEvent implements UniversalUnpunishEvent {

	private final boolean auto;
	
	public PostUnpunishEvent(final Punishment punishment) {
		this(punishment, false);
	}
	
	public PostUnpunishEvent(final Punishment punishment, final boolean auto) {
		super(punishment);
		this.auto = auto;
	}

	@Override
	public boolean isAutomatic() {
		return auto;
	}
	
}
