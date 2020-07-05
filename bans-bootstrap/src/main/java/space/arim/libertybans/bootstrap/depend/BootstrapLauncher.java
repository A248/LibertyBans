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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
	
	private boolean informErrorOrReturnTrue(DownloadResult result) {
		switch (result.getResultType()) {
		case HASH_MISMATCH:
			errorMessage("Failed to download dependency: " + result.getDependency() + " . Reason: Hash mismatch, "
					+ "expected " + bytesToHex(result.getExpectedHash()) + " but got " + bytesToHex(result.getActualHash()));
			return false;
		case ERROR:
			errorMessage("Failed to download dependency: " + result.getDependency() + " . Reason: Exception");
			result.getException().printStackTrace(System.err);
			return false;
		default:
			break;
		}
		return true;
	}
	
	private static URL[] toURLs(File file) throws MalformedURLException {
		File[] input = file.listFiles();
		URL[] urls = new URL[input.length];
		for (int n = 0; n < input.length; n++) {
			urls[n] = input[n].toURI().toURL();
		}
		return urls;
	}
	
	private CompletableFuture<Boolean> loadApi() {
		return apiDepLoader.execute().thenApply((results) -> {
			for (DownloadResult result : results) {
				if (!informErrorOrReturnTrue(result)) {
					return false;
				}
			}
			try {
				for (URL url : toURLs(apiDepLoader.getOutputDirectory())) {
					ADD_URL_METHOD.invoke(apiClassLoader, url);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| MalformedURLException ex) {
				errorMessage("Failed to attach dependencies to API ClassLoader");
				ex.printStackTrace(System.err);
				return false;
			}
			return true;
		});
	}
	
	private CompletableFuture<Boolean> loadInternal() {
		return internalDepLoader.execute().thenApply((results) -> {
			for (DownloadResult result : results) {
				if (!informErrorOrReturnTrue(result)) {
					return false;
				}
			}
			try {
				for (URL url : toURLs(internalDepLoader.getOutputDirectory())) {
					internalClassLoader.addURL(url);
				}
			} catch (MalformedURLException ex) {
				errorMessage("Failed to attach dependencies to internal ClassLoader");
				ex.printStackTrace(System.err);
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
