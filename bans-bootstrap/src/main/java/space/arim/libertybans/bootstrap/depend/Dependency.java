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

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * A dependency, with both identifying details and some related information
 * 
 * @author A248
 *
 */
public final class Dependency {

	private final String groupId;
	private final String artifactId;
	private final String version;
	private transient final byte[] sha512hash;

	private Dependency(String groupId, String artifactId, String version, byte[] sha512hash) {
		this.groupId = Objects.requireNonNull(groupId, "groupId");
		this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
		this.version = Objects.requireNonNull(version, "version");
		this.sha512hash = sha512hash.clone();
	}


	/**
	 * Creates a dependency
	 *
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @param version the version
	 * @param hexHash the SHA 512 hash, null to disable hash checking
	 * @return the dependency
	 */
	public static Dependency of(String groupId, String artifactId, String version, String hexHash) {
		return new Dependency(
				groupId, artifactId, version,
				hexStringToByteArray(hexHash.toLowerCase(Locale.ROOT))
		);
	}

	// https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
	static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toLowerCase(Locale.ROOT).toCharArray();
	static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return String.valueOf(hexChars);
	}
	
	public String getFullName() {
		return groupId + '.' + artifactId + '_' + version;
	}
	
	public String groupId() {
		return groupId;
	}
	
	public String artifactId() {
		return artifactId;
	}
	
	public String version() {
		return version;
	}

	public byte[] getSha512Hash() {
		return sha512hash.clone();
	}

	public boolean matchesHash(byte[] otherSha512Hash) {
		return Arrays.equals(sha512hash, otherSha512Hash);
	}

	DownloadResult hashMismatchResult(byte[] actualHash) {
		return DownloadResult.hashMismatch0(sha512hash, actualHash);
	}

	@Override
	public String toString() {
		return "Dependency [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + artifactId.hashCode();
		result = prime * result + groupId.hashCode();
		result = prime * result + version.hashCode();
		return result;
	}

	/**
	 * A dependency is considered equal to another depending on the groupId,
	 * artifactId, and version
	 *
	 * @param object the other object
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Dependency)) {
			return false;
		}
		Dependency other = (Dependency) object;
		return groupId.equals(other.groupId) && artifactId.equals(other.artifactId) && version.equals(other.version);
	}
	
}
