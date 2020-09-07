/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

final class Revision implements Comparable<Revision> {
	
	final int major;
	final int minor;
	
	private Revision(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}
	
	static Revision of(int major, int minor) {
		return new Revision(major, minor);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + major;
		result = prime * result + minor;
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Revision)) {
			return false;
		}
		Revision other = (Revision) object;
		return major == other.major && minor == other.minor;
	}

	@Override
	public String toString() {
		return major + "." + minor;
	}

	@Override
	public int compareTo(Revision o) {
		int majorDiff = major - o.major;
		if (majorDiff == 0) {
			return minor - o.minor;
		}
		return majorDiff;
	}
	
}
