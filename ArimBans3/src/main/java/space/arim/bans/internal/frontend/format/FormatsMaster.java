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

import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.internal.Component;

public interface FormatsMaster extends Component {
	@Override
	default Class<?> getType() {
		return FormatsMaster.class;
	}
	
	boolean useJson();
	
	String formatMessageWithPunishment(String message, Punishment punishment);
	
	String formatPunishment(Punishment punishment);
	
	String formatSubject(Subject subj);
	
	String formatNotification(Punishment punishment, boolean add, Subject operator);
	
	default String formatTime(long millis) {
		return formatTime(millis, true);
	}
	
	String formatTime(long millis, boolean absolute);
	
	long toMillis(String timespan);
	
	String getConsoleDisplay();
	
	boolean isCmdMuteBlocked(String cmd);
	
}
