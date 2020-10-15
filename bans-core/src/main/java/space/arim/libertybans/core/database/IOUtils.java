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
package space.arim.libertybans.core.database;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class IOUtils {
	
	private IOUtils() {}

	private static InputStream getResource(String resourceName) throws IOException {
		URL url = IOUtils.class.getResource('/' + resourceName);
		if (url == null) {
			throw new IllegalArgumentException("Resource " + resourceName + " not found");
		}
		return url.openStream();
	}
	
	/**
	 * Blocking operation which reads all the content of a resource
	 * 
	 * @param resourceName the resource name
	 * @return the resource content
	 * @throws UncheckedIOException if an IO error occurred
	 */
	static ByteArrayOutputStream readResource(String resourceName) {
		try (InputStream is = getResource(resourceName)) {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			is.transferTo(baos);
			return baos;
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to read internal resource " + resourceName, ex);
		}
	}
	
	/**
	 * Blocking operation which reads all SQL queries from the specified resource name. <br>
	 * This is otherwise equivalent to reading a resource file, excluding lines starting with
	 * {@literal "--"} and blank lines, and splitting the result by ";". <br>
	 * <br>
	 * The returned list is mutable.
	 * 
	 * @param resourceName the resource name
	 * @return a mutable list of SQL queries
	 * @throws UncheckedIOException if an IO error occurred
	 */
	static List<String> readSqlQueries(String resourceName) {
		try (InputStream inputStream = getResource(resourceName);
				InputStreamReader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(inputReader)) {

			List<String> result = new ArrayList<>();
			StringBuilder currentBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("--") || line.isBlank()) {
					continue;
				}
				currentBuilder.append(line);

				if (line.endsWith(";")) {
					result.add(currentBuilder.substring(0, currentBuilder.length() - 1));
					currentBuilder = new StringBuilder();
					continue;
				} else {
					currentBuilder.append('\n');
				}
			}
			return result;
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to read internal resource " + resourceName, ex);
		}
	}
	
}
