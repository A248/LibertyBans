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
package space.arim.api.util.web;

import java.util.Objects;

import space.arim.bans.api.exception.RateLimitException;

public class RateLimitedService {
	
	private final String name;
	private final String genericUrl;
	private final long refreshTime;
	
	private long expireTime = 0;
	
	public RateLimitedService(final String name, final String genericUrl, final long refreshTime) {
		this.name = name;
		this.genericUrl = genericUrl;
		this.refreshTime = refreshTime;
		expireTime = System.currentTimeMillis() - refreshTime;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUrl(String address) throws RateLimitException {
		if (isRateLimiting()) {
			throw new RateLimitException(name);
		}
		return genericUrl.replace("$ADDR", Objects.requireNonNull(address, "Address must not be null!"));
	}
	
	private boolean isRateLimiting() {
		return System.currentTimeMillis()/1000000L - expireTime < refreshTime;
	}
	
	public void expire() throws RateLimitException {
		expireTime = System.currentTimeMillis()/1000000L;
		throw new RateLimitException(name);
	}
	
}
