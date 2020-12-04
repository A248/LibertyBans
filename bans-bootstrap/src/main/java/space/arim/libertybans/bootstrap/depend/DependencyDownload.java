/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap.depend;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class DependencyDownload {

	private final Dependency dependency;
	private final Repository repository;
	private final Path outputJar;

	DependencyDownload(Dependency dependency, Repository repository, Path outputJar) {
		this.dependency = dependency;
		this.repository = repository;
		this.outputJar = outputJar;
	}

	DownloadResult download() {
		URL url;
		try {
			url = repository.locateDependency(dependency);
		} catch (MalformedURLException ex) {
			return DownloadResult.exception(ex);
		}

		// Get MessageDigest instance
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException ex) {
			return DownloadResult.exception(ex);
		}
		// Read all bytes from download stream
		byte[] jarBytes;
		try (InputStream inputStream = url.openStream();
				DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
			jarBytes = digestInputStream.readAllBytes();

		} catch (IOException ex) {
			return DownloadResult.exception(ex);
		}

		// Compare hash to expected hash
		byte[] actualHash = digest.digest();
		if (!dependency.matchesHash(actualHash)) {
			return dependency.hashMismatchResult(actualHash);
		}

		// Write to file
		try (FileChannel fc = FileChannel.open(outputJar, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			fc.write(ByteBuffer.wrap(jarBytes));
		} catch (IOException ex) {
			return DownloadResult.exception(ex);
		}
		return DownloadResult.success(outputJar);
	}

}
