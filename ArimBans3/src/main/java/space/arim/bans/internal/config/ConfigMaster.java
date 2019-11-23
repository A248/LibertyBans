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
package space.arim.bans.internal.config;

import java.util.List;

import space.arim.bans.internal.Component;

public interface ConfigMaster extends Component {
	@Override
	default Class<?> getType() {
		return ConfigMaster.class;
	}
	
	String getConfigString(String key);

	List<String> getConfigStrings(String key);

	boolean getConfigBoolean(String key);

	int getConfigInt(String key);
	
	String getMessagesString(String key);
	
	List<String> getMessagesStrings(String key);
	
	List<Integer> getMessagesInts(String key);
}
