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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DefaultDependencyLoader implements DependencyLoader {

	private Executor executor;
	private Map<Dependency, Repository> pairs = new HashMap<>();
	private Path outputDir;
	
	public DefaultDependencyLoader() {

	}
	
	@Override
	public DependencyLoader setExecutor(Executor executor) {
		this.executor = executor;
		return this;
	}

	@Override
	public DependencyLoader addPair(Dependency dependency, Repository repository) {
		pairs.put(dependency, repository);
		return this;
	}

	@Override
	public DependencyLoader setOutputDirectory(Path outputDir) {
		try {
			Files.createDirectories(outputDir);
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot create directory " + outputDir, ex);
		}
		this.outputDir = outputDir;
		return this;
	}
	
	@Override
	public Path getOutputDirectory() {
		return outputDir;
	}
	
	private CompletableFuture<DownloadResult> downloadDependency(Dependency dependency, Repository repository) {
		return CompletableFuture.supplyAsync(() -> {

			Path outputJar = outputDir.resolve(dependency.getFullName() + ".jar");
			if (Files.exists(outputJar)) {
				return DownloadResult.success(outputJar);
			}
			String urlPath = repository.getBaseUrl() + '/' + dependency.groupId().replace('.', '/') + '/'
					+ dependency.artifactId() + '/' + dependency.version() + '/' + dependency.artifactId() + '-'
					+ dependency.version() + ".jar";
			URL url;
			try {
				url = new URL(urlPath);
			} catch (MalformedURLException ex) {
				return DownloadResult.exception(ex);
			}

			// Get MessageDigest instance
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("SHA-512");
			} catch (NoSuchAlgorithmException ex) {
				return DownloadResult.exception(ex);
			}
			// Read all bytes from download stream
			byte[] jarBytes;
			try (InputStream is = url.openStream();
					DigestInputStream dis = new DigestInputStream(is, md)) {
				jarBytes = dis.readAllBytes();

			} catch (IOException ex) {
				return DownloadResult.exception(ex);
			}

			// Compare hash to expected hash
			byte[] actualHash = md.digest();
			byte[] expectedHash = dependency.getSha512Hash();
			if (!Arrays.equals(expectedHash, actualHash)) {
				if (expectedHash != null) {
					return DownloadResult.hashMismatch(expectedHash, actualHash);
				}
				System.out.println("Warning: Disabled hash comparison. Actual hash is " + Arrays.toString(actualHash));
			}

			// Write to file
			try (FileChannel fc = FileChannel.open(outputJar)) {
				fc.write(ByteBuffer.wrap(jarBytes));
			} catch (IOException ex) {
				return DownloadResult.exception(ex);
			}
			return DownloadResult.success(outputJar);
		}, executor);
	}

	@Override
	public CompletableFuture<Map<Dependency, DownloadResult>> execute() {
		Map<Dependency, CompletableFuture<DownloadResult>> futures = new HashMap<>(pairs.size());
		for (Entry<Dependency, Repository> pair : pairs.entrySet()) {
			futures.put(pair.getKey(), downloadDependency(pair.getKey(), pair.getValue()));
		}
		/*
		 * Now, all we have to do is convert a map with futures to a future of a map
		 */
		return CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).thenApply((ignore) -> {

			Map<Dependency, DownloadResult> result = new HashMap<>();
			for (Entry<Dependency, CompletableFuture<DownloadResult>> entry : futures.entrySet()) {
				// Will not block, because the future must already be complete
				result.put(entry.getKey(), entry.getValue().join());
			}
			return result;
		});
	}

}
