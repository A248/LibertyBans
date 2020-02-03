/* 
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended;

import java.util.List;

import space.arim.universal.util.lang.AutoClosable;

public interface ArimBansExtendedPlugin extends AutoClosable {
	
	ArimBansExtended extension();
	
	default boolean enabled() {
		return extension() != null;
	}
	
	@Override
	default void close() {
		if (extension() != null) {
			extension().close();
		}
	}
	
	List<String> getTabComplete(String[] args);
	
}
