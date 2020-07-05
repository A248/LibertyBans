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

public class DownloadResult {

	private final ResultType resultType;
	private final Dependency dependency;
	private final byte[] expectedHash;
	private final byte[] actualHash;
	private final Exception ex;
	
	private DownloadResult(ResultType resultType, Dependency dependency, byte[] expectedHash, byte[] actualHash, Exception ex) {
		this.resultType = resultType;
		this.dependency = dependency;
		this.expectedHash = expectedHash;
		this.actualHash = actualHash;
		this.ex = ex;
	}
	
	private DownloadResult(ResultType resultType, Dependency dependency, Exception ex) {
		this(resultType, dependency, null, null, ex);
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
	 * Gets the dependency which was downloaded or attempted to be downloaded
	 * 
	 * @return the dependency
	 */
	public Dependency getDependency() {
		return dependency;
	}
	
	public byte[] getExpectedHash() {
		return expectedHash;
	}
	
	public byte[] getActualHash() {
		return actualHash;
	}
	
	/**
	 * Gets the exception associated with this result, or {@code null} for none
	 * 
	 * @return
	 */
	public Exception getException() {
		return ex;
	}
	
	public static DownloadResult success(Dependency dependency) {
		return new DownloadResult(ResultType.SUCCESS, dependency, null);
	}
	
	public static DownloadResult hashMismatch(Dependency dependency, byte[] expected, byte[] actual) {
		return new DownloadResult(ResultType.HASH_MISMATCH, dependency, expected, actual, null);
	}
	
	public static DownloadResult exception(Dependency dependency, Exception ex) {
		return new DownloadResult(ResultType.ERROR, dependency, ex);
	}
	
	public enum ResultType {
		SUCCESS,
		HASH_MISMATCH,
		ERROR
	}
	
}
