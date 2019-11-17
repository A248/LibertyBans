/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.frontend.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;

//TODO Make this class work
public class Formats implements FormatsMaster {
	private ArimBans center;
	private SimpleDateFormat dateformatter;

	public Formats(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	@Override
	public String format(Punishment punishment) {
		
		return center.config().getString("messages");
	}
	
	@Override
	public String format(Subject subj) {
		return null;
	}

	@Override
	public String fromUnix(long unix) {
		return dateformatter.format(new Date(unix));
	}
	
	@Override
	public long toUnix(String timespan) {
		return -1L;
	}
	
	@Override
	public void close() {
		
	}
	
	@Override
	public void refreshConfig() {
		dateformatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		dateformatter.setTimeZone(TimeZone.getDefault());
	}

}
