/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.config;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.api.punish.Punishment;

public interface InternalFormatter extends PunishmentFormatter {

	/**
	 * Parses a message to send without any prefix
	 * 
	 * @param messageToParse the message to parse
	 * @return the message parsed without any prefix
	 */
	SendableMessage parseMessageWithoutPrefix(String messageToParse);
	
	/**
	 * Prefixes a message and returns the prefixed result
	 * 
	 * @param message the message
	 * @return the message including the prefix
	 */
	SendableMessage prefix(SendableMessage message);
	
	/**
	 * Gets, formats, and parses the punishment message for a punishment.
	 * 
	 * @param punishment the punishment
	 * @return a future yielding the formatted sendable message
	 */
	CentralisedFuture<SendableMessage> getPunishmentMessage(Punishment punishment);
	
	/**
	 * Parses and formats a message with a punishment
	 * 
	 * @param manipulator the message manipulator
	 * @param punishment the punishment
	 * @return a future of the resulting formatted sendable message
	 */
	CentralisedFuture<SendableMessage> formatWithPunishment(SendableMessageManipulator manipulator,
			Punishment punishment);
	
	/**
	 * Parses and formats a message with a punishment and an undoing operator. Used when punishments are revoked.
	 * 
	 * @param manipulator the message manipulator
	 * @param punishment the punishment
	 * @param unOperator the operator undoing the punishment
	 * @return a future of the resulting formatted sendable message
	 */
	CentralisedFuture<SendableMessage> formatWithPunishmentAndUnoperator(SendableMessageManipulator manipulator,
			Punishment punishment, Operator unOperator);
	
}
