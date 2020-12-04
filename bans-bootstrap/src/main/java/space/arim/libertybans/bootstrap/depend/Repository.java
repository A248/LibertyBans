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
package space.arim.libertybans.bootstrap.depend;

import java.net.MalformedURLException;
import java.net.URL;

public interface Repository {
	
	String getBaseUrl();
	
	/**
	 * Gets the URL pointing to a dependency, assuming it is in this repository
	 * 
	 * @param dependency the dependency
	 * @return a url pointing to it
	 * @throws MalformedURLException if the url is malformed for whatever reason
	 */
	default URL locateDependency(Dependency dependency) throws MalformedURLException {
		String urlPath = getBaseUrl() + '/' + dependency.groupId().replace('.', '/') + '/'
				+ dependency.artifactId() + '/' + dependency.version() + '/' + dependency.artifactId() + '-'
				+ dependency.version() + ".jar";
		return new URL(urlPath);
	}
	
}
