/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.selector.cache;

import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.punish.Punishment;

import java.util.Objects;

final class MuteAndMessage {

	private final Punishment mute;
	private final Component message;

	MuteAndMessage(Punishment mute, Component message) {
		this.mute = Objects.requireNonNull(mute, "mute");
		this.message = Objects.requireNonNull(message, "message");
	}

	Punishment mute() {
		return mute;
	}

	Component message() {
		return message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MuteAndMessage that = (MuteAndMessage) o;
		return mute.equals(that.mute) && message.equals(that.message);
	}

	@Override
	public int hashCode() {
		int result = mute.hashCode();
		result = 31 * result + message.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "MuteAndMessage{" +
				"mute=" + mute +
				", message=" + message +
				'}';
	}
}
