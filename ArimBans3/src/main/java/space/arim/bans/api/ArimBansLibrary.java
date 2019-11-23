/* 
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import java.util.UUID;
import java.util.logging.Logger;

import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.util.Tools;

public interface ArimBansLibrary extends PunishmentPlugin, AutoCloseable {

	/**
	 * Gets a Subject from a UUID
	 * 
	 * @param uuid - the uuid to turn into a subject
	 * @return Subject representing the player UUID
	 */
	Subject fromUUID(UUID uuid);
	
	/**
	 * Gets a Subject from an IP Address
	 * <br><br>Use {@link #checkAddress(String)} to validate the address first.
	 * 
	 * @param address - the address to use
	 * @return a Subject representing the address
	 * @throws IllegalArgumentException if address is invalid
	 */
	Subject fromIpAddress(String address) throws IllegalArgumentException;
	
	/**
	 * Gets a Subject from arbitrary user input
	 * 
	 * <br><br>Will automatically detect if IP address or UUID. Short UUIDs will be automatically expanded.
	 * 
	 * @param input - the String to convert to a Subject
	 * @return Subject representing the input specified
	 */
	Subject parseSubject(String input);
	
	/**
	 * Simulates execution of a command
	 * 
	 * @param subject - the player (or console) executing the command
	 * @param command - the command to be executed
	 * @param args - additional arguments
	 */
	void simulateCommand(Subject subject, CommandType command, String[] args);
	
	/**
	 * Use this to get a UUID from a playername
	 * <br><br>If you have the option to execute asynchronously,
	 * use {@link #resolveName(String)} instead.
	 * 
	 * @param name - the name to be looked up
	 * @return the uuid of the corresponding player
	 * @throws PlayerNotFoundException if no player by that name is cached
	 */
	UUID uuidFromName(String name) throws PlayerNotFoundException;
	
	/**
	 * Use this to get a playername from a UUID
	 * <br><br>If you have the option to execute asynchronously,
	 * use {@link #resolveUUID(UUID)} instead.
	 * 
	 * @param uuid - the uuid to be looked up
	 * @return the name of the corresponding player
	 * @throws PlayerNotFoundException if no player by that uuid is cached
	 */
	String nameFromUUID(UUID uuid) throws PlayerNotFoundException;
	
	/**
	 * Use this to get a UUID from a playername in an async thread
	 * Differs from {@link #uuidFromName(UUID)} in that this method will query the Ashcon/Mojang APIs.
	 * 
	 * <br><br><b>This is a blocking operation.</b>
	 * 
	 * @param name - the name to be resolved
	 * @return the uuid of the corresponding player
	 * @throws PlayerNotFoundException if the name could not be resolved to a uuid
	 */
	UUID resolveName(String name) throws PlayerNotFoundException;
	
	/**
	 * Use this to get a playername from a UUID in an async thread
	 * Differs from {@link #nameFromUUID(UUID)} in that this method will query the Ashcon/Mojang APIs.
	 * 
	 * <br><br><b>This is a blocking operation.</b>
	 * 
	 * @param uuid - the uuid to be resolved
	 * @return the name of the corresponding player
	 * @throws PlayerNotFoundException if the uuid could not be resolved to a name
	 */
	String resolveUUID(UUID uuid) throws PlayerNotFoundException;
	
	/**
	 * Executes a block of code asynchronously.
	 * 
	 * <br><br>Bukkit users may call {@link org.bukkit.scheduler.BukkitScheduler#runTask(Plugin, Runnable) Bukkit.getScheduler().runTask(Plugin, Runnable)} to resynchronize.
	 * 
	 * @param task - the lambda/runnable to run async
	 */
	void async(Runnable task);
	
	/**
	 * Gets the internal ArimBans logger.
	 * 
	 * <br><br>This log does not write to server console but instead outputs to the plugin's data folder.
	 * 
	 * @return Logger the logger
	 */
	Logger getLogger();
	
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
	 * @param address - the address to check
	 * @return true if the address is valid, false otherwise
	 */
	default boolean checkAddress(String address) {
		return Tools.validAddress(address);
	}
	
}
