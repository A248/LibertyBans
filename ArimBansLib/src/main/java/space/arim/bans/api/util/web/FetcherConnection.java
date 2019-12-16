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
package space.arim.bans.api.util.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import space.arim.bans.api.exception.HttpStatusException;

public class FetcherConnection implements AutoCloseable {

	private final String url;
	private HttpURLConnection connection;
	
	FetcherConnection(String url) {
		this.url = url;
	}
	
	FetcherConnection connect() throws IOException, HttpStatusException {
		connection = (HttpURLConnection) (new URL(url)).openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		HttpStatus status = HttpStatus.fromCode(connection.getResponseCode());
		if (status != HttpStatus.OK) {
			throw new HttpStatusException(status);
		}
		return this;
	}
	
	InputStream inputStream() throws IOException {
		return connection.getInputStream();
	}
	
	JSONObject toJson() throws IOException, ParseException {
		return (JSONObject) JSONValue.parseWithException(new InputStreamReader(inputStream()));
	}
	
	@Override
	public void close() {
		connection.disconnect();
	}
	
}
