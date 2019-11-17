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
package space.arim.bans.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.google.common.net.InetAddresses;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import space.arim.bans.api.exception.FetcherException;

public class Tools {
	
	private static final String ASHCON_API = "https://api.ashcon.app/mojang/v2/user/";
	private static final String MOJANG_API_FROM_NAME = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String MOJANG_API_FROM_UUID = "https://api.mojang.com/user/profiles/";
	
    private static final String SPIGOT_UPDATE_API = "https://api.spigotmc.org/legacy/update.php?resource=";
	
	private Tools() {}
	
	private static JSONObject getDataFromUrl(String url) throws ProtocolException, IOException, ParseException, FetcherException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		if (connection.getResponseCode() == 204) {
			throw new FetcherException("Mojang API response code = 204 (no content)");
		}
		JSONObject json = (JSONObject) JSONValue.parseWithException(new InputStreamReader(connection.getInputStream()));
		connection.disconnect();
		return json;
	}
	
	public static UUID mojangApi(final String name) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		return UUID.fromString(expandUUID(getDataFromUrl(MOJANG_API_FROM_NAME + name).get("id").toString()));
	}
	
	public static String mojangApi(final UUID playeruuid) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(MOJANG_API_FROM_UUID + playeruuid.toString().replaceAll("-", "") + "/names")).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		if (connection.getResponseCode() == 204) {
			throw new FetcherException("Mojang API response code = 204 (no content)");
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
		return UUID.fromString(getDataFromUrl(ASHCON_API + name).get("uuid").toString());
	}
	
	public static String ashconApi(final UUID playeruuid) throws ProtocolException, MalformedURLException, IOException, ParseException, FetcherException {
		return getDataFromUrl("https://api.ashcon.app/mojang/v2/user/" + playeruuid).get("username").toString();
	}
	
	public static String getLatestVersion(int resourceId) throws ProtocolException, MalformedURLException, IOException, FetcherException {
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
	
	public static String expandUUID(String uuid) {
		return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16)
		+ "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32);
	}
	
	public static boolean validAddress(String address) {
		return InetAddresses.isInetAddress(address);
	}
	
	private enum TagType {
		NONE,
		TTP,
		URL,
		CMD,
		SGT
	}
	
	private static TagType jsonTag(String node) {
		if (node.length() < 5) {
			return TagType.NONE;
		}
		switch (node.substring(0, 4)) {
		case "ttp:":
			return TagType.TTP;
		case "url:":
			return TagType.URL;
		case "cmd:":
			return TagType.CMD;
		case "sgt:":
			return TagType.SGT;
		default:
			return TagType.NONE;
		}
	}
	
	public static String stripJson(String json) {
		StringBuilder builder = new StringBuilder();
		for (String s : json.split("||")) {
			if (jsonTag(s).equals(TagType.NONE)) {
				builder.append(s);
			}
		}
		return builder.toString();
	}
	
	public static String encode(String colorable) {
		char[] b = colorable.toCharArray();
		for (int n = 0; n < b.length - 1; ++n) {
			if (b[n] == '&' && "0123456789abcdefklmnor".indexOf(b[n + 1]) > -1) {
				b[n] = 167;
			}
		}
		return new String(b);
	}
	
	public static BaseComponent[] parseJson(String json, boolean color) {
		BaseComponent current = null;
		ArrayList<BaseComponent> components = new ArrayList<BaseComponent>();
		for (String node : json.split("||")) {
			TagType tag = jsonTag(node);
			if (tag.equals(TagType.NONE)) {
				if (current != null) {
					components.add(current);
				}
				if (color) {
					current = new TextComponent(TextComponent.fromLegacyText(encode(node)));
				} else {
					current = new TextComponent(node);
				}
			} else if (current != null) {
				String value;
				if (color) {
					value = encode(node.substring(3));
				} else {
					value = node.substring(3);
				}
				if (tag.equals(TagType.TTP)) {
					current.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(value)));
				} else if (tag.equals(TagType.URL)) {
					current.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, value));
				} else if (tag.equals(TagType.CMD)) {
					current.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, value));
				} else if (tag.equals(TagType.SGT)) {
					current.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value));
				}
			}
		}
		return (BaseComponent[]) components.toArray();
	}
	
	public static BaseComponent[] parseJson(String json) {
		return parseJson(json, true);
	}
	
	public static boolean generateFile(File file) {
		if (file.exists() && file.canRead() && file.canWrite()) {
			return true;
		} else if (file.exists()) {
			file.delete();
		}
		if (!file.getParentFile().mkdirs()) {
			return false;
		}
		try {
			return file.createNewFile();
		} catch (IOException ex) {
			return false;
		}
	}
}
