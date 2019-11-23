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

public class Web {
	
	private static final String ASHCON_API = "https://api.ashcon.app/mojang/v2/user/";
	private static final String MOJANG_API_FROM_NAME = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String MOJANG_API_FROM_UUID = "https://api.mojang.com/user/profiles/";
	
    private static final String SPIGOT_UPDATE_API = "https://api.spigotmc.org/legacy/update.php?resource=";
    
    private static final String IPSTACK_API = "http://api.ipstack.com/";
    private static final String FREE_GEOIP_API = "https://freegeoip.app/json/";
    
    private static final String ERROR_204 = "Response code = 204 (no content)";
	
	private Web() {}
	
	private static JSONObject getDataFromUrl(String url) throws ProtocolException, IOException, ParseException, FetcherException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		if (connection.getResponseCode() == 204) {
			throw new FetcherException(ERROR_204);
		}
		JSONObject json = (JSONObject) JSONValue.parseWithException(new InputStreamReader(connection.getInputStream()));
		connection.disconnect();
		return json;
	}
	
	public static UUID mojangApi(final String name) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		Objects.requireNonNull(name, "Name must not be null!");
		return UUID.fromString(Tools.expandUUID(getDataFromUrl(MOJANG_API_FROM_NAME + name).get("id").toString()));
	}
	
	public static String mojangApi(final UUID playeruuid) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		Objects.requireNonNull(playeruuid, "UUID must not be null!");
		HttpURLConnection connection = (HttpURLConnection) (new URL(MOJANG_API_FROM_UUID + playeruuid.toString().replaceAll("-", "") + "/names")).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		if (connection.getResponseCode() == 204) {
			throw new FetcherException(ERROR_204);
		}
		try (Scanner scanner = new Scanner(connection.getInputStream())) {
			String s = scanner.useDelimiter("\\A").next();
			connection.disconnect();
			scanner.close();
			return ((JSONObject) JSONValue.parseWithException(s.substring(s.lastIndexOf('{'), s.lastIndexOf('}') + 1))).get("name").toString();
		} catch (Exception ex) {
			throw ex;
		}
	}
	
	public static UUID ashconApi(final String name) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		Objects.requireNonNull(name, "Name must not be null!");
		return UUID.fromString(getDataFromUrl(ASHCON_API + name).get("uuid").toString());
	}
	
	public static String ashconApi(final UUID playeruuid) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		Objects.requireNonNull(playeruuid, "UUID must not be null!");
		return getDataFromUrl("https://api.ashcon.app/mojang/v2/user/" + playeruuid).get("username").toString();
	}
	
	public static String getLatestPluginVersion(final int resourceId) throws ProtocolException, MalformedURLException, IOException, FetcherException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(SPIGOT_UPDATE_API + resourceId)).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		try (Scanner scanner = new Scanner(connection.getInputStream())) {
			if (scanner.hasNext()) {
				String version = scanner.next();
				connection.disconnect();
				scanner.close();
				return version;
			}
			throw new FetcherException("Scanner has no response!");
		} catch (Exception ex) {
			throw ex;
		}
	}
	
	public static JSONObject ipStack(final String address, final String key) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		Objects.requireNonNull(address, "Address must not be null!");
		Objects.requireNonNull(key, "Key must not be null!");
		return getDataFromUrl(IPSTACK_API + address + "?access_key=" + key);
	}
	
	public static JSONObject freeGeoIp(final String address) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		Objects.requireNonNull(address, "Address must not be null!");
		return getDataFromUrl(FREE_GEOIP_API + address);
	}
	
}
