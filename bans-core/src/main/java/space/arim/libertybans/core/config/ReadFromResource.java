/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ReadFromResource {

	private final URL resource;

	public ReadFromResource(URL resource) {
		this.resource = Objects.requireNonNull(resource, "resource");
	}

	/**
	 * Creates from an absolute resource path
	 *
	 * @param resource the absolute resource path
	 */
	public ReadFromResource(String resource) {
		this(ReadFromResource.class.getResource("/" + resource));
	}

	public <V> V read(ReadFunction<Reader, V> readFunction) {
		try (InputStream sqlStream = resource.openStream();
			 InputStreamReader reader = new InputStreamReader(sqlStream, StandardCharsets.UTF_8)) {
			return readFunction.readFrom(reader);

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public <V> V readBuffered(ReadFunction<? super BufferedReader, V> readFunction) {
		class BufferingReadFunction implements ReadFunction<Reader, V> {

			@Override
			public V readFrom(Reader inputReader) throws IOException {
				try (BufferedReader bufferedReader = new BufferedReader(inputReader)) {
					return readFunction.readFrom(bufferedReader);
				}
			}
		}
		return read(new BufferingReadFunction());

	}

	public interface ReadFunction<R extends Reader, V> {

		V readFrom(R reader) throws IOException;
	}

}
