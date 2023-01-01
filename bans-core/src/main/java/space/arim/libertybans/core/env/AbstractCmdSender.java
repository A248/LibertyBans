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

package space.arim.libertybans.core.env;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.env.AudienceRepresenter;
import space.arim.api.env.annote.PlatformCommandSender;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.core.config.InternalFormatter;

public abstract class AbstractCmdSender<@PlatformCommandSender C> implements CmdSender {

	private final InternalFormatter formatter;
	private final Interlocutor interlocutor;
	private final AudienceRepresenter<C> audienceRepresenter;
	private final C rawSender;
	private final Operator operator;

	protected AbstractCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
								AudienceRepresenter<C> audienceRepresenter, C rawSender, Operator operator) {
		this.formatter = formatter;
		this.interlocutor = interlocutor;
		this.audienceRepresenter = audienceRepresenter;
		this.rawSender = rawSender;
		this.operator = operator;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public void sendMessageNoPrefix(ComponentLike message) {
		Audience audience = audienceRepresenter.toAudience(getRawSender());
		audience.sendMessage(
				interlocutor.filterIpAddresses(this, message)
		);
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

	protected C getRawSender() {
		return rawSender;
	}

}
