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

package space.arim.libertybans.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.Victim;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

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
	Component parseMessageWithoutPrefix(String messageToParse);
	
	/**
	 * Prefixes a message and returns the prefixed result
	 * 
	 * @param message the message
	 * @return the message including the prefix
	 */
	ComponentLike prefix(ComponentLike message);
	
	/**
	 * Gets, formats, and parses the punishment message for a punishment.
	 * 
	 * @param punishment the punishment
	 * @return a future yielding the formatted sendable message
	 */
	CentralisedFuture<Component> getPunishmentMessage(Punishment punishment);
	
	/**
	 * Parses and formats a message with a punishment
	 * 
	 * @param componentText the message
	 * @param punishment the punishment
	 * @return a future of the resulting formatted sendable message
	 */
	CentralisedFuture<Component> formatWithPunishment(ComponentText componentText,
													  Punishment punishment);
	
	/**
	 * Parses and formats a message with a punishment and an undoing operator. Used when punishments are revoked.
	 * 
	 * @param componentText the message
	 * @param punishment the punishment
	 * @param unOperator the operator undoing the punishment
	 * @return a future of the resulting formatted sendable message
	 */
	CentralisedFuture<Component> formatWithPunishmentAndUnoperator(ComponentText componentText,
																   Punishment punishment,
																   Operator unOperator);

	/**
	 * Formats the value of a victim variable
	 *
	 * @param victim the victim
	 * @return the formatted victim
	 */
	CentralisedFuture<String> formatVictim(Victim victim);

	/**
	 * Formats the value of an operator variable
	 *
	 * @param operator the operator
	 * @return the formatted operator
	 */
	CentralisedFuture<String> formatOperator(Operator operator);

}
