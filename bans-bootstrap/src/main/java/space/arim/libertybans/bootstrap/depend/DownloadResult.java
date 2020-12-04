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

import java.nio.file.Path;

public class DownloadResult {

	private final ResultType resultType;
	private final Path jarFile;
	private final byte[] expectedHash;
	private final byte[] actualHash;
	private final Exception ex;
	
	private DownloadResult(ResultType resultType, Path jarFile, byte[] expectedHash, byte[] actualHash, Exception ex) {
		this.resultType = resultType;
		this.jarFile = jarFile;
		this.expectedHash = expectedHash;
		this.actualHash = actualHash;
		this.ex = ex;
	}
	
	private DownloadResult(ResultType resultType, Path jarFile, Exception ex) {
		this(resultType, jarFile, null, null, ex);
	}
	
	/**
	 * Gets the result type of the result
	 * 
	 * @return the result type
	 */
	public ResultType getResultType() {
		return resultType;
	}
	
	/**
	 * Gets the jar file which the download is saved to,
	 * or null if the download failed.
	 * 
	 * @return the jar file
	 */
	public Path getJarFile() {
		return jarFile;
	}
	
	/**
	 * Gets the expected hash or {@code null} if this result is not a hash mismatch
	 * 
	 * @return the expected hash or null
	 */
	public byte[] getExpectedHash() {
		return (expectedHash == null) ? null : expectedHash.clone();
	}
	
	/**
	 * Gets the actual hash or {@code null} if this result is not a hash mismatch
	 * 
	 * @return the actual hash or null
	 */
	public byte[] getActualHash() {
		return (actualHash == null) ? null : actualHash.clone();
	}
	
	/**
	 * Gets the exception associated with this result, or {@code null} for none
	 * 
	 * @return the exception or {@code null}
	 */
	public Exception getException() {
		return ex;
	}
	
	public static DownloadResult success(Path outputJar) {
		return new DownloadResult(ResultType.SUCCESS, outputJar, null);
	}
	
	public static DownloadResult hashMismatch(byte[] expected, byte[] actual) {
		return hashMismatch0(expected.clone(), actual.clone());
	}
	
	static DownloadResult hashMismatch0(byte[] expected, byte[] actual) {
		return new DownloadResult(ResultType.HASH_MISMATCH, null, expected, actual, null);
	}
	
	public static DownloadResult exception(Exception ex) {
		return new DownloadResult(ResultType.ERROR, null, ex);
	}
	
	public enum ResultType {
		SUCCESS,
		HASH_MISMATCH,
		ERROR
	}
	
}
