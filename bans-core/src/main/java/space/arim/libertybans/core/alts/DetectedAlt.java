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

package space.arim.libertybans.core.alts;

import space.arim.libertybans.api.NetworkAddress;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DetectedAlt {

	private final DetectionKind detectionKind;
	private final NetworkAddress relevantAddress;
	private final UUID relevantUserId;
	private final String relevantUserName;
	private final Instant dateAccountRecorded;

	public DetectedAlt(DetectionKind detectionKind, NetworkAddress relevantAddress,
					   UUID relevantUserId, String relevantUserName, Instant dateAccountRecorded) {
		this.detectionKind = Objects.requireNonNull(detectionKind, "detectionKind");
		this.relevantAddress = Objects.requireNonNull(relevantAddress, "relevantAddress");
		this.relevantUserId = Objects.requireNonNull(relevantUserId, "relevantUserId");
		this.relevantUserName = Objects.requireNonNull(relevantUserName, "relevantUserName");
		this.dateAccountRecorded = Objects.requireNonNull(dateAccountRecorded, "dateAccountRecorded");
	}

	public DetectionKind detectionKind() {
		return detectionKind;
	}

	public NetworkAddress relevantAddress() {
		return relevantAddress;
	}

	public UUID relevantUserId() {
		return relevantUserId;
	}

	public String relevantUserName() {
		return relevantUserName;
	}

	public Instant dateAccountRecorded() {
		return dateAccountRecorded;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DetectedAlt that = (DetectedAlt) o;
		return detectionKind == that.detectionKind && relevantAddress.equals(that.relevantAddress) && relevantUserId.equals(that.relevantUserId) && relevantUserName.equals(that.relevantUserName) && dateAccountRecorded.equals(that.dateAccountRecorded);
	}

	@Override
	public int hashCode() {
		int result = detectionKind.hashCode();
		result = 31 * result + relevantAddress.hashCode();
		result = 31 * result + relevantUserId.hashCode();
		result = 31 * result + relevantUserName.hashCode();
		result = 31 * result + dateAccountRecorded.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "DetectedAlt{" +
				"detectionKind=" + detectionKind +
				", relevantAddress=" + relevantAddress +
				", relevantUserId=" + relevantUserId +
				", relevantUserName='" + relevantUserName + '\'' +
				", dateAccountRecorded=" + dateAccountRecorded +
				'}';
	}
}
