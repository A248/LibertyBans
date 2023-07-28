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

package space.arim.libertybans.core.database.jooq;

import org.jetbrains.annotations.NotNull;
import org.jooq.Converter;
import space.arim.libertybans.api.punish.EscalationTrack;

public final class EscalationTrackConverter implements Converter<String, EscalationTrack> {

	@Override
	public EscalationTrack from(String databaseObject) {
		if (databaseObject == null) {
			return null;
		}
		String[] parts = databaseObject.split(":");
		if (parts.length != 2) {
			throw new IllegalStateException("Received " + databaseObject + " which is not a valid escalation track");
		}
		return EscalationTrack.create(parts[0], parts[1]);
	}

	@Override
	public String to(EscalationTrack userObject) {
		if (userObject == null) {
			return null;
		}
		return userObject.getNamespace() + ':' + userObject.getValue();
	}

	@Override
	public @NotNull Class<String> fromType() {
		return String.class;
	}

	@Override
	public @NotNull Class<EscalationTrack> toType() {
		return EscalationTrack.class;
	}

}
