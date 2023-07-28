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

package space.arim.libertybans.env.standalone;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.api.jsonchat.adventure.implementor.MessageOnlyAudience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ConsoleReceiver(List<String> messages) implements ConsoleAudience, MessageOnlyAudience {

	public ConsoleReceiver() {
		this(Collections.synchronizedList(new ArrayList<>()));
	}

	@Override
	public void sendMessage(@NonNull Identity source, @NonNull Component message, @NonNull MessageType type) {
		messages.add(PlainComponentSerializer.plain().serialize(message));
	}

	@Override
	public UnsupportedOperationException notSupportedException() {
		return new UnsupportedOperationException("Not supported");
	}
}
