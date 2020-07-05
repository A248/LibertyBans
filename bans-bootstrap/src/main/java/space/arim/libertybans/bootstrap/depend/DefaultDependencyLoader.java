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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
	private File outputDir;
	
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
	public DependencyLoader setOutputDirectory(File outputDir) {
		if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
			throw new IllegalStateException("Cannot create output directory " + outputDir);
		}
		this.outputDir = outputDir;
		return this;
	}
	
	@Override
	public File getOutputDirectory() {
		return outputDir;
	}
	
	private CompletableFuture<DownloadResult> downloadDependency(Dependency dependency, Repository repository) {
		return CompletableFuture.supplyAsync(() -> {
			File outputJar = new File(outputDir, dependency.getFullName() + ".jar");
			if (outputJar.exists()) {
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
			byte[] jarBytes;
			try (InputStream is = url.openStream()) {
				jarBytes = is.readAllBytes();

			} catch (IOException ex) {
				return DownloadResult.exception(ex);
			}
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("SHA-512");
			} catch (NoSuchAlgorithmException ex) {
				return DownloadResult.exception(ex);
			}
			byte[] actualHash = md.digest(jarBytes);
			if (!Arrays.equals(actualHash, dependency.getSha512Hash())) {
				return DownloadResult.hashMismatch(dependency.getSha512Hash(), actualHash);
			}
			try (FileOutputStream fos = new FileOutputStream(outputJar)) {
				fos.write(jarBytes);
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
		return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[] {})).thenApply((ignore) -> {

			Map<Dependency, DownloadResult> result = new HashMap<>();
			for (Entry<Dependency, CompletableFuture<DownloadResult>> entry : futures.entrySet()) {
				// Will not block, because the future must already be complete
				result.put(entry.getKey(), entry.getValue().join());
			}
			return result;
		});
	}

}
