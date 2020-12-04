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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
		Path outputJar = outputDir.resolve(dependency.getFullName() + ".jar");
		if (Files.exists(outputJar)) {
			return CompletableFuture.completedFuture(DownloadResult.success(outputJar));
		}
		return CompletableFuture.supplyAsync(() -> {
			return new DependencyDownload(dependency, repository, outputJar).download();
		}, executor);
	}

	@Override
	public CompletableFuture<Map<Dependency, DownloadResult>> execute() {
		try {
			Files.createDirectories(outputDir);
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot create directory " + outputDir, ex);
		}
		Map<Dependency, CompletableFuture<DownloadResult>> futures = new HashMap<>(pairs.size());
		for (Entry<Dependency, Repository> pair : pairs.entrySet()) {
			futures.put(pair.getKey(), downloadDependency(pair.getKey(), pair.getValue()));
		}
		// Convert a map with futures to a future of a map
		return CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).thenApply((ignore) -> {

			Map<Dependency, DownloadResult> result = new HashMap<>(futures.size());
			for (Entry<Dependency, CompletableFuture<DownloadResult>> entry : futures.entrySet()) {
				// Will not block, because the future must already be complete
				result.put(entry.getKey(), entry.getValue().join());
			}
			return Map.copyOf(result);
		});
	}

}
