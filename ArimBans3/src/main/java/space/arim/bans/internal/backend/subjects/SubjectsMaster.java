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
package space.arim.bans.internal.backend.subjects;

import java.util.UUID;

import space.arim.bans.api.Subject;
import space.arim.bans.internal.Component;

public interface SubjectsMaster extends Component {
	
	@Override
	default Class<?> getType() {
		return SubjectsMaster.class;
	}
	
	boolean isOnline(Subject subject);
	
	Subject parseSubject(String input, boolean console) throws IllegalArgumentException;
	
	default Subject parseSubject(String input) throws IllegalArgumentException {
		return parseSubject(input, true);
	}
	
	Subject parseSubject(UUID input);
	
	default void sendMessage(Subject subject, String...jsonables) {
		sendMessage(subject, false, jsonables);
	}
	
	void sendMessage(Subject subject, boolean prefixed, String...jsonables);
	
	boolean hasPermission(Subject subject, String permission);
	
	boolean checkUUID(UUID uuid);

}
