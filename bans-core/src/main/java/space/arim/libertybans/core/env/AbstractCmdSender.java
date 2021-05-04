/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.env;

import jakarta.inject.Inject;

import jakarta.inject.Singleton;
import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.annote.PlatformCommandSender;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;

public abstract class AbstractCmdSender implements CmdSender {
	
	private final CmdSenderHelper senderHelper;
	private final Object rawSender;
	private final Operator operator;
	
	protected AbstractCmdSender(CmdSenderHelper senderHelper, Object rawSender, Operator operator) {
		this.senderHelper = senderHelper;
		this.rawSender = rawSender;
		this.operator = operator;
	}
	
	@Singleton
	public static class CmdSenderHelper {
		
		private final Configs configs;
		final InternalFormatter formatter;
		final PlatformHandle handle;
		
		@Inject
		public CmdSenderHelper(Configs configs, InternalFormatter formatter, PlatformHandle handle) {
			this.configs = configs;
			this.formatter = formatter;
			this.handle = handle;
		}

		SendableMessage addPrefix(SendableMessage message) {
			return configs.getMessagesConfig().all().prefix().concatenate(message);
		}
		
	}
	
	@Override
	public Operator getOperator() {
		return operator;
	}
	
	@Override
	public void sendMessageNoPrefix(SendableMessage message) {
		senderHelper.handle.sendMessage(rawSender, message);
	}
	
	@Override
	public void sendMessage(SendableMessage message) {
		sendMessageNoPrefix(senderHelper.addPrefix(message));
	}
	
	@Override
	public void sendLiteralMessageNoPrefix(String message) {
		sendMessageNoPrefix(senderHelper.formatter.parseMessageWithoutPrefix(message));
	}
	
	@Override
	public void sendLiteralMessage(String message) {
		sendMessage(senderHelper.formatter.parseMessageWithoutPrefix(message));
	}
	
	@Override
	@PlatformCommandSender
	public Object getRawSender() {
		return rawSender;
	}
	
}
