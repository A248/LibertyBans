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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import space.arim.bans.api.exception.FetcherException;
import space.arim.bans.api.exception.RateLimitException;

public final class Fetcher {
	
	private static final String ASHCON_API = "https://api.ashcon.app/mojang/v2/user/";
	private static final String MOJANG_API_FROM_NAME = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String MOJANG_API_FROM_UUID = "https://api.mojang.com/user/profiles/";
	
    private static final String SPIGOT_UPDATE_API = "https://api.spigotmc.org/legacy/update.php?resource=";
    
    private static final String IPSTACK_API = "http://api.ipstack.com/";
    private static final String FREE_GEOIP_API = "https://freegeoip.app/json/";
    
	private Fetcher() {}
	
	private static JSONObject getDataFromUrl(String url) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		HttpStatus status = HttpStatus.fromCode(connection.getResponseCode());
		if (status != HttpStatus.OK) {
			throw new FetcherException(status);
		}
		JSONObject json = (JSONObject) JSONValue.parseWithException(new InputStreamReader(connection.getInputStream()));
		connection.disconnect();
		return json;
	}
	
	public static UUID mojangApi(final String name) throws FetcherException {
		Objects.requireNonNull(name, "Name must not be null!");
		final String url = MOJANG_API_FROM_NAME + name;
		JSONObject json;
		try {
			json = getDataFromUrl(url);
		} catch (IOException | ParseException ex) {
			throw new FetcherException("Could not connect to " + url, ex);
		}
		return UUID.fromString(Tools.expandUUID(json.get("id").toString()));
	}
	
	public static String mojangApi(final UUID playeruuid) throws FetcherException {
		Objects.requireNonNull(playeruuid, "UUID must not be null!");
		final String url = MOJANG_API_FROM_UUID + playeruuid.toString().replaceAll("-", "") + "/names";
		HttpURLConnection connection;
		HttpStatus status;
		try {
			connection = (HttpURLConnection) (new URL(url)).openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			status = HttpStatus.fromCode(connection.getResponseCode());
		} catch (IOException ex) {
			throw new FetcherException("Could not connect to " + url, ex);
		}
		if (status != HttpStatus.OK) {
			throw new FetcherException(status);
		}
		try (Scanner scanner = new Scanner(connection.getInputStream())) {
			String s = scanner.useDelimiter("\\A").next();
			connection.disconnect();
			scanner.close();
			return ((JSONObject) JSONValue.parseWithException(s.substring(s.lastIndexOf('{'), s.lastIndexOf('}') + 1))).get("name").toString();
		} catch (ParseException | IOException ex) {
			throw new FetcherException("Could parse JSON from " + url, ex);
		}
	}
	
	public static UUID ashconApi(final String name) throws FetcherException {
		Objects.requireNonNull(name, "Name must not be null!");
		final String url = ASHCON_API + name;
		JSONObject json;
		try {
			json = getDataFromUrl(url);
		} catch (IOException | ParseException ex) {
			throw new FetcherException("Could not connect to " + url, ex);
		}
		return UUID.fromString(json.get("uuid").toString());
	}
	
	public static String ashconApi(final UUID playeruuid) throws FetcherException {
		Objects.requireNonNull(playeruuid, "UUID must not be null!");
		final String url = ASHCON_API + playeruuid;
		try {
			return getDataFromUrl(url).get("username").toString();
		} catch (IOException | ParseException ex) {
			throw new FetcherException("Could not connect to " + url, ex);
		}
	}
	
	public static String getLatestPluginVersion(final int resourceId) throws FetcherException {
		final String url = SPIGOT_UPDATE_API + resourceId;
		HttpURLConnection connection;
		HttpStatus status;
		try {
			connection = (HttpURLConnection) (new URL(url)).openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			status = HttpStatus.fromCode(connection.getResponseCode());
		} catch (IOException ex) {
			throw new FetcherException("Could not connect to " + url, ex);
		}
		if (status != HttpStatus.OK) {
			throw new FetcherException(status);
		}
		try (Scanner scanner = new Scanner(connection.getInputStream())) {
			if (scanner.hasNext()) {
				String version = scanner.next();
				connection.disconnect();
				scanner.close();
				return version;
			}
			throw new FetcherException("Scanner has no response!");
		} catch (IOException ex) {
			throw new FetcherException("Could not connect to " + url, ex);
		}
	}
	
	public static GeoIpInfo ipStack(final String address, final String key) throws FetcherException, RateLimitException {
		Objects.requireNonNull(address, "Address must not be null!");
		Objects.requireNonNull(key, "Key must not be null!");
		final String url = IPSTACK_API + address + "?access_key=" + key + "&fields=country_code,country_name,region_code,region_name,city,zip,latitude,longitude";
		JSONObject json;
		try {
			json = getDataFromUrl(url);
		} catch (IOException | ParseException ex) {
			throw new FetcherException("Could not connect to " + url);
		}
		if (json.containsKey("success")) {
			if (!(Boolean) json.get("success")) {
				throw new RateLimitException("IpStack");
			}
		}
		try {
			return new GeoIpInfo(address, json.get("country_code").toString(), json.get("country_name").toString(), json.get("region_code").toString(), json.get("region_name").toString(), json.get("city").toString(), json.get("zip").toString(), Double.parseDouble(json.get("latitude").toString()), Double.parseDouble(json.get("longitude").toString()));
		} catch (NumberFormatException ex) {
			throw new FetcherException("Could not parse JSON from " + url, ex);
		}
	}
	
	public static GeoIpInfo freeGeoIp(final String address) throws FetcherException, RateLimitException {
		Objects.requireNonNull(address, "Address must not be null!");
		final String url = FREE_GEOIP_API + address;
		JSONObject json;
		try {
			json = getDataFromUrl(url);
		} catch (IOException | ParseException ex) {
			throw new FetcherException("Could not connect to " + url);
		} catch (FetcherException ex) {
			if (ex.code == HttpStatus.FORBIDDEN) {
				throw new RateLimitException("FreeGeoIp");
			}
			throw ex;
		}
		try {
			return new GeoIpInfo(address, json.get("country_code").toString(), json.get("country_name").toString(), json.get("region_code").toString(), json.get("region_name").toString(), json.get("city").toString(), json.get("zip_code").toString(), Double.parseDouble(json.get("latitude").toString()), Double.parseDouble(json.get("longitude").toString()));
		} catch (NumberFormatException ex) {
			throw new FetcherException("Could not parse JSON from " + url, ex);
		}
	}
	
}
