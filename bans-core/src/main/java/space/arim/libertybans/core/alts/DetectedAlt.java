/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.user.AltAccount;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record DetectedAlt(UUID uuid, String username, NetworkAddress address, Instant recorded,
						  DetectionKind detectionKind, Set<PunishmentType> scannedTypes)
		implements AltAccount {

	public DetectedAlt {
		Objects.requireNonNull(uuid, "uuid");
		Objects.requireNonNull(address, "address");
		Objects.requireNonNull(recorded, "recorded");
		Objects.requireNonNull(detectionKind, "detectionKind");
		scannedTypes = Set.copyOf(scannedTypes);
	}

	public DetectedAlt(UUID uuid, String username, NetworkAddress address, Instant lastObserved,
					   DetectionKind detectionKind, PunishmentType...scannedTypes) {
		this(uuid, username, address, lastObserved, detectionKind, Set.of(scannedTypes));
	}

	@Override
	public Optional<String> latestUsername() {
		return Optional.ofNullable(username);
	}

	@Override
	public boolean hasActivePunishment(PunishmentType type) {
		return scannedTypes.contains(type);
	}

}
