/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import java.util.UUID;

import space.arim.bans.api.exception.MissingPunishmentException;

import space.arim.universal.util.AutoClosable;

import space.arim.api.sql.SQLExecution;
import space.arim.api.util.StringsUtil;

/**
 * A {@link PunishmentPlugin} with specific support for ArimBans' features.
 * 
 * @author A248
 *
 */
public interface ArimBansLibrary extends PunishmentPlugin, SQLExecution, AutoClosable {

	/**
	 * Used internally for invalid messages. If you send a message whose sole content
	 * is this, you will receive an unchecked exception.
	 */
	static String INVALID_STRING_CODE = "<ArimBans_Invalid_String_Code:_If_You_See_This_Please_Tell_An_Admin_Immediately>";
	
	/**
	 * Checks a message for validity
	 * <br><br>All this does is:
	 * <br>1. Check against {@link #INVALID_STRING_CODE} for equality
	 * <br>2. Check for null values
	 * 
	 * @param message the string to check
	 * @return true if valid
	 */
	static boolean checkString(String message) {
		return message != null && !INVALID_STRING_CODE.equals(message);
	}
	
	/**
	 * Gets a Subject from a UUID
	 * 
	 * @param uuid the uuid to turn into a subject
	 * @return Subject representing the player UUID
	 */
	Subject fromUUID(UUID uuid);
	
	/**
	 * Gets a Subject from an IP Address
	 * <br><br>Use {@link #checkAddress(String)} to validate the address first.
	 * 
	 * @param address the address to use
	 * @return a Subject representing the address
	 * @throws IllegalArgumentException if address is invalid
	 */
	Subject fromIpAddress(String address) throws IllegalArgumentException;
	
	/**
	 * Gets a Subject from arbitrary user input
	 * 
	 * <br><br>Will automatically detect if IP address or UUID. Short UUIDs will be automatically expanded.
	 * 
	 * @param input the String to convert to a Subject
	 * @return Subject representing the input specified
	 * @throws IllegalArgumentException if input does not match to any type of Subject
	 */
	Subject parseSubject(String input) throws IllegalArgumentException;
	
	/**
	 * Simulates execution of a command
	 * 
	 * @param subject the player (or console) executing the command
	 * @param command the command to be executed
	 * @param args additional arguments
	 */
	void simulateCommand(Subject subject, CommandType command, String[] args);
	
	/**
	 * Simulates execution of a command. Differs from {@link #simulateCommand(Subject, CommandType, String[])} in that it automatically parses command types. <br>
	 * 
	 * @param subject the player (or console) executing the command
	 * @param args the arguments including the command itself
	 */
	void simulateCommand(Subject subject, String[] args);
	
	/**
	 * Simulates execution of a command. Differs from {@link #simulateCommand(Subject, String[])} in that it accepts a generic String representing the command arguments. <br>
	 * <br>
	 * Example usage if <code>banLib</code> represents an ArimBansLibrary object: <br>
	 * <code>banLib.simulateCommand(banLib.fromUUID("some uuid"), "ipban player15 19d ip-banned for 19 days");</code>
	 * 
	 * @param subject the player (or console) executing the command
	 * @param rawArgs the arguments including the command itself
	 */
	default void simulateCommand(Subject subject, String rawArgs) {
		simulateCommand(subject, rawArgs.split(" "));
	}
	
	/**
	 * Changes a punishment reason
	 * 
	 * @param punishment the punishment whose reason to change
	 * @param reason the new reason
	 * @throws MissingPunishmentException if the punishment is neither found in active punishments nor history
	 */
	void changeReason(Punishment punishment, String reason) throws MissingPunishmentException;
	
	/**
	 * Executes a block of code asynchronously.
	 * 
	 * <br><br>Bukkit/Spigot users may call Bukkit.getScheduler().runTask(Plugin, Runnable) to resynchronize.
	 * 
	 * @param task the lambda/runnable to run async
	 */
	void async(Runnable task);
	
	/**
	 * Reloads whole plugin configuration, including config.yml and messages.yml
	 */
	void reload();
	
	/**
	 * Reloads functional configuration (config.yml)
	 */
	void reloadConfig();
	
	/**
	 * Reloads message configuration (messages.yml)
	 */
	void reloadMessages();
	
	/**
	 * Checks to ensure an address is valid.
	 * 
	 * <br><br>Relies on {@link com.google.common.net.InetAddresses#isInetAddress(String) InetAddress.isInetAddress}
	 * 
	 * @param address the address to check
	 * @return true if the address is valid, false otherwise
	 */
	default boolean checkAddress(String address) {
		return StringsUtil.validAddress(address);
	}
	
}
