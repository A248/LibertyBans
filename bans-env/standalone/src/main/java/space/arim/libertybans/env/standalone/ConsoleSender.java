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

import jakarta.inject.Inject;
import net.kyori.adventure.text.ComponentLike;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;

import java.util.stream.Stream;

final class ConsoleSender implements CmdSender {

	private final InternalFormatter formatter;
	private final ConsoleAudience audience;

	@Inject
	ConsoleSender(InternalFormatter formatter, ConsoleAudience audience) {
		this.formatter = formatter;
		this.audience = audience;
	}

	@Override
	public Operator getOperator() {
		return ConsoleOperator.INSTANCE;
	}

	@Override
	public boolean hasPermission(String permission) {
		return true;
	}

	@Override
	public void sendMessageNoPrefix(ComponentLike message) {
		audience.sendMessage(message);
	}

	@Override
	public void sendMessage(ComponentLike message) {
		sendMessageNoPrefix(formatter.prefix(message));
	}

	@Override
	public void sendLiteralMessageNoPrefix(String message) {
		sendMessageNoPrefix(formatter.parseMessageWithoutPrefix(message));
	}

	@Override
	public void sendLiteralMessage(String message) {
		sendMessage(formatter.parseMessageWithoutPrefix(message));
	}

	@Override
	public Stream<String> getPlayerNames() {
		return Stream.empty();
	}

	@Override
	public Stream<String> getPlayerNamesOnSameServer() {
		return Stream.empty();
	}

}
