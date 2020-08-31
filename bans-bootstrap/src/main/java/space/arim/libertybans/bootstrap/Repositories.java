/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import space.arim.libertybans.bootstrap.depend.Repository;

final class Repositories {

	static final Repository ARIM_LESSER_GPL3 = new Repository("https://mvn-repo.arim.space/lesser-gpl3");
	static final Repository ARIM_GPL3 = new Repository("https://mvn-repo.arim.space/gpl3");
	static final Repository ARIM_AFFERO_GPL3 = new Repository("https://mvn-repo.arim.space/affero-gpl3");
	
	static final Repository CENTRAL_REPO = new Repository("https://repo.maven.apache.org/maven2");
	
}
