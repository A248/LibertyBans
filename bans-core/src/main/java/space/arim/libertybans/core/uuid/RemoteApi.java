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
package space.arim.libertybans.core.uuid;

import java.net.http.HttpClient;
import java.util.Locale;

import space.arim.api.util.web.HttpAshconApi;
import space.arim.api.util.web.HttpMcHeadsApi;
import space.arim.api.util.web.HttpMojangApi;
import space.arim.api.util.web.RemoteNameHistoryApi;

/**
 * Pseudo enum of remote web APIs
 * 
 * @author A248
 *
 */
class RemoteApi {
	
	private static final RemoteApi MOJANG;
	private static final RemoteApi ASHCON;
	private static final RemoteApi MCHEADS;
	
	static {
		HttpClient httpClient = HttpClient.newHttpClient();
		MOJANG = new RemoteApi(new HttpMojangApi(httpClient));
		ASHCON = new RemoteApi(new HttpAshconApi(httpClient));
		MCHEADS = new RemoteApi(new HttpMcHeadsApi(httpClient));
	}
	
	private final RemoteNameHistoryApi remote;
	
	private RemoteApi(RemoteNameHistoryApi remote) {
		this.remote = remote;
	}
	
	RemoteNameHistoryApi getRemote() {
		return remote;
	}
	
	@Override
	public String toString() {
		String name = remote.getClass().getSimpleName();
		return name.substring(4, name.length() - 3);
	}

	static RemoteApi nullableValueOf(String configOpt) {
		switch (configOpt.toUpperCase(Locale.ENGLISH)) {
		case "MOJANG":
			return MOJANG;
		case "ASHCON":
			return ASHCON;
		case "MCHEADS":
			return MCHEADS;
		default:
			return null;
		}
	}
	
}
