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

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.annote.PlatformCommandSender;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;

public abstract class AbstractCmdSender implements CmdSender {
	
	private final AbstractDependencies dependencies;
	private final Object rawSender;
	private final Operator operator;
	
	protected AbstractCmdSender(AbstractDependencies dependencies, Object rawSender, Operator operator) {
		this.dependencies = dependencies;
		this.rawSender = rawSender;
		this.operator = operator;
	}
	
	public static class AbstractDependencies {
		
		final Configs configs;
		final InternalFormatter formatter;
		final PlatformHandle handle;
		
		@Inject
		public AbstractDependencies(Configs configs, InternalFormatter formatter, PlatformHandle handle) {
			this.configs = configs;
			this.formatter = formatter;
			this.handle = handle;
		}
		
	}
	
	@Override
	public Operator getOperator() {
		return operator;
	}
	
	@Override
	public void sendMessageNoPrefix(SendableMessage message) {
		dependencies.handle.sendMessage(rawSender, message);
	}
	
	@Override
	public void sendMessage(SendableMessage message) {
		SendableMessage prefix = dependencies.configs.getMessagesConfig().all().prefix();
		message = prefix.concatenate(message);
		sendMessageNoPrefix(message);
	}
	
	@Override
	public void sendLiteralMessageNoPrefix(String message) {
		sendMessageNoPrefix(dependencies.formatter.parseMessageWithoutPrefix(message));
	}
	
	@Override
	public void sendLiteralMessage(String message) {
		sendMessage(dependencies.formatter.parseMessageWithoutPrefix(message));
	}
	
	@Override
	@PlatformCommandSender
	public Object getRawSender() {
		return rawSender;
	}
	
}
