/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.importing;

public final class ImportStatistics {

	private int active;
	private int historical;
	private int nameAddressRecord;

	private boolean failed;

	public ImportStatistics() {}

	public ImportStatistics(int active, int historical, int nameAddressRecord) {
		this.active = active;
		this.historical = historical;
		this.nameAddressRecord = nameAddressRecord;
	}

	public boolean success() {
		return !failed;
	}

	void markFailed() {
		failed = true;
	}

	void transferredActive() {
		active++;
	}

	void transferredHistorical() {
		historical++;
	}

	void transferredNameAddressRecord() {
		nameAddressRecord++;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ImportStatistics that = (ImportStatistics) o;
		return active == that.active && historical == that.historical
				&& nameAddressRecord == that.nameAddressRecord && failed == that.failed;
	}

	@Override
	public int hashCode() {
		int result = active;
		result = 31 * result + historical;
		result = 31 * result + nameAddressRecord;
		result = 31 * result + (failed ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		String failure = (failed) ? "(FAILED) " : "";
		return "-- Import statistics " + failure + "-- \n" +
				"Active punishments: " + active + "\n" +
				"Historical punishments: " + historical + "\n" +
				"Name or address history records: " + nameAddressRecord;
	}
}
