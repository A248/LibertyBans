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
package space.arim.bans.api.util;

public class GeoIpInfo {
	
	public final String address;
	public final String country_code;
	public final String country_name;
	public final String region_code;
	public final String region_name;
	public final String city;
	public final String zip;
	public final double latitude;
	public final double longitude;
	
	GeoIpInfo(String address, String country_code, String country_name, String region_code, String region_name, String city, String zip, double latitude, double longitude) {
		this.address = address;
		this.country_code = country_code;
		this.country_name = country_name;
		this.region_code = region_code;
		this.region_name = region_name;
		this.city = city;
		this.zip = zip;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
}
