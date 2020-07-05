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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Must use System.err in this class because we do not know whether the platform uses slf4j or JUL
 * 
 * @author A248
 *
 */
public class BootstrapLauncher {
	
	private final String programName;
	
	private final URLClassLoader apiClassLoader;
	private final AddableURLClassLoader internalClassLoader;
	
	private final DependencyLoader apiDepLoader;
	private final DependencyLoader internalDepLoader;
	
	private static final Method ADD_URL_METHOD;
	
	static {
		Method addUrlMethod;
		try {
			addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			addUrlMethod.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException ex) {
			throw new ExceptionInInitializerError(ex);
		}
		ADD_URL_METHOD = addUrlMethod;
	}
	
	public BootstrapLauncher(String programName, URLClassLoader apiClassLoader, DependencyLoader apiDepLoader, DependencyLoader internalDepLoader) {
		this.programName = programName;
		this.apiClassLoader = apiClassLoader;
		internalClassLoader = new AddableURLClassLoader(programName, apiClassLoader);

		this.apiDepLoader = apiDepLoader;
		this.internalDepLoader = internalDepLoader;
	}
	
	public URLClassLoader getApiClassLoader() {
		return apiClassLoader;
	}
	
	public URLClassLoader getInternalClassLoader() {
		return internalClassLoader;
	}
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	private boolean informErrorOrReturnTrue(Dependency dependency, DownloadResult result) {
		switch (result.getResultType()) {
		case HASH_MISMATCH:
			errorMessage("Failed to download dependency: " + dependency + " . Reason: Hash mismatch, "
					+ "expected " + bytesToHex(result.getExpectedHash()) + " but got " + bytesToHex(result.getActualHash()));
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
	
	private CompletableFuture<URL[]> loadURLs(DependencyLoader loader) {
		return loader.execute().thenApply((results) -> {
			URL[] urls = new URL[results.size()];
			int n = 0;
			try {
				for (Map.Entry<Dependency, DownloadResult> entry : results.entrySet()) {
					if (!informErrorOrReturnTrue(entry.getKey(), entry.getValue())) {
						return null;
					}
					urls[n++] = entry.getValue().getJarFile().toURI().toURL();
				}
			} catch (MalformedURLException ex) {
				ex.printStackTrace(System.err);
				return null;
			}
			return urls;
		});
	}
	
	private CompletableFuture<Boolean> loadApi() {
		return loadURLs(apiDepLoader).thenApply((urls) -> {
			if (urls == null) {
				return false;
			}
			try {
				for (URL url : urls) {
					ADD_URL_METHOD.invoke(apiClassLoader, url);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				errorMessage("Failed to attach dependencies to API ClassLoader");
				ex.printStackTrace(System.err);
				return false;
			}
			return true;
		});
	}
	
	private CompletableFuture<Boolean> loadInternal() {
		return loadURLs(internalDepLoader).thenApply((urls) -> {
			if (urls == null) {
				return false;
			}
			for (URL url : urls) {
				internalClassLoader.addURL(url);
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
