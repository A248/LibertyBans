/*
 * LibertyBans-bootstrap
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */

package space.arim.libertybans.bootstrap;

import space.arim.libertybans.bootstrap.depend.Repository;

enum Repositories implements Repository {

	ARIM_LESSER_GPL3("https://dl.cloudsmith.io/public/anand-beh/lesser-gpl3/maven"),
	ARIM_GPL3("https://dl.cloudsmith.io/public/anand-beh/gpl3/maven"),
	ARIM_AFFERO_GPL3("https://dl.cloudsmith.io/public/anand-beh/affero-gpl3/maven"),

	CENTRAL_REPO("https://repo.maven.apache.org/maven2");

	private final String baseUrl;

	/**
	 * Creates from a base URL
	 * 
	 * @param baseUrl the base URL, without the trailing slash
	 */
	private Repositories(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public String getBaseUrl() {
		return baseUrl;
	}

}
