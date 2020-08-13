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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Composition of 2 {@link DependencyLoader}s used to add dependencies to an external URLClassLoader and
 * internal AddableURLClassLoader. The external URLClassLoader may require reflection to access its {@code addURL}
 * method. {@code addUrlsToExternalClassLoader} may be overriden to change this behaviour.
 * 
 * @author A248
 *
 */
public class BootstrapLauncher {
	
	private final String programName;
	
	private final ClassLoader apiClassLoader;
	private final AddableURLClassLoader internalClassLoader;
	
	private final DependencyLoader apiDepLoader;
	private final DependencyLoader internalDepLoader;
	
	private final BiFunction<ClassLoader, Path[], Boolean> addUrlsToExternal;
	
	public BootstrapLauncher(String programName, ClassLoader apiClassLoader, DependencyLoader apiDepLoader,
			DependencyLoader internalDepLoader, BiFunction<ClassLoader, Path[], Boolean> addUrlsToExternal) {
		this.programName = programName;
		this.apiClassLoader = apiClassLoader;
		internalClassLoader = new AddableURLClassLoader(programName, apiClassLoader);

		this.apiDepLoader = apiDepLoader;
		this.internalDepLoader = internalDepLoader;

		this.addUrlsToExternal = addUrlsToExternal;
	}
	
	public DependencyLoader getApiDepLoader() {
		return apiDepLoader;
	}
	
	public DependencyLoader getInternalDepLoader() {
		return internalDepLoader;
	}
	
	public ClassLoader getApiClassLoader() {
		return apiClassLoader;
	}
	
	public AddableURLClassLoader getInternalClassLoader() {
		return internalClassLoader;
	}
	
	private boolean informErrorOrReturnTrue(Dependency dependency, DownloadResult result) {
		switch (result.getResultType()) {
		case HASH_MISMATCH:
			errorMessage("Failed to download dependency: " + dependency + " . Reason: Hash mismatch, " + "expected "
					+ Dependency.bytesToHex(result.getExpectedHash()) + " but got "
					+ Dependency.bytesToHex(result.getActualHash()));
			return false;
		case ERROR:
			errorMessage("Failed to download dependency: " + dependency + " . Reason: Exception");
			result.getException().printStackTrace(System.err);
			return false;
		default:
			break;
		}
		return true;
	}
	
	private CompletableFuture<Path[]> loadPaths(DependencyLoader loader) {
		return loader.execute().thenApply((results) -> {
			Path[] paths = new Path[results.size()];
			int n = 0;
			for (Map.Entry<Dependency, DownloadResult> entry : results.entrySet()) {
				if (!informErrorOrReturnTrue(entry.getKey(), entry.getValue())) {
					return null;
				}
				paths[n++] = entry.getValue().getJarFile();
			}
			return paths;
		});
	}
	
	private CompletableFuture<Boolean> loadApi() {
		return loadPaths(apiDepLoader).thenApply((paths) -> {
			if (paths == null) {
				return false;
			}
			return addUrlsToExternal.apply(apiClassLoader, paths);
		});
	}
	
	private CompletableFuture<Boolean> loadInternal() {
		return loadPaths(internalDepLoader).thenApply((paths) -> {
			if (paths == null) {
				return false;
			}
			try {
				for (Path path : paths) {
					internalClassLoader.addURL(path.toUri().toURL());
				}
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
				return false;
			}
			return true;
		});
	}
	
	// Must use System.err because we do not know whether the platform uses slf4j or JUL
	private void errorMessage(String message) {
		System.err.println('[' + programName + "] " + message);
	}
	
	public CompletableFuture<Boolean> loadAll() {
		CompletableFuture<Boolean> apiFuture = loadApi();
		CompletableFuture<Boolean> internalFuture = loadInternal();
		return apiFuture.thenCombine(internalFuture, (r1, r2) -> r1 && r2);
	}

}
