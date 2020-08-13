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
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

class DefaultDependencyLoader implements DependencyLoader {

	private final Executor executor;
	private final Map<Dependency, Repository> pairs;
	private final Path outputDir;
	
	DefaultDependencyLoader(Executor executor, Map<Dependency, Repository> pairs, Path outputDir) {
		this.executor = executor;
		this.pairs = Map.copyOf(pairs);
		this.outputDir = outputDir;
	}
	
	@Override
	public Executor getExecutor() {
		return executor;
	}

	@Override
	public Map<Dependency, Repository> getDependencyPairs() {
		return pairs;
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
			if (!dependency.matchesHash(actualHash)) {
				return DownloadResult.hashMismatch(dependency.getSha512Hash(), actualHash);
			}

			// Write to file
			try (FileChannel fc = FileChannel.open(outputJar, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
				fc.write(ByteBuffer.wrap(jarBytes));
			} catch (IOException ex) {
				return DownloadResult.exception(ex);
			}
			return DownloadResult.success(outputJar);
		}, executor);
	}

	@Override
	public CompletableFuture<Map<Dependency, DownloadResult>> execute() {
		try {
			Files.createDirectories(outputDir);
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot create directory " + outputDir, ex);
		}
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
