/* 
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.api.Subject;

import space.arim.universal.registry.Registry;
import space.arim.universal.util.collections.CollectionsUtil;

import space.arim.api.annotation.RequireRegistration;
import space.arim.api.config.SimpleConfig;
import space.arim.api.util.StringsUtil;

public class ArimBansExtended extends SimpleConfig {
	
	private static final String[] COMMANDS = {"ban", "unban", "ipban", "ipunban", "mute", "unmute", "ipmute", "ipunmute", "warn", "unwarn", "ipwarn", "ipunwarn", "kick", "ipkick", "banlist", "ipbanlist", "playerbanlist", "mutelist", "ipmutelist", "playermutelist", "history", "iphistory", "warns", "ipwarns", "status", "ipstatus", "ips", "geoip", "alts", "blame", "rollback"};
	
	private final ArimBansLibrary lib;
	private final File folder;
	
	private final ConcurrentHashMap<String, Object> cfg = new ConcurrentHashMap<String, Object>();
	
	ArimBansExtended(@RequireRegistration(PunishmentPlugin.class) Registry registry, File folder) {
		super(folder, "do-not-touch-version");
		PunishmentPlugin plugin = registry.getRegistration(PunishmentPlugin.class);
		if (plugin instanceof ArimBansLibrary) {
			lib = (ArimBansLibrary) plugin;
		} else {
			throw new IllegalStateException("No PunishmentPlugin/ArimBansLibrary registration found!");
		}
		this.folder = Objects.requireNonNull(folder, "Folder must not be null!");
	}
	
	public ArimBansLibrary getLib() {
		return lib;
	}
	
	File getFolder() {
		return folder;
	}
	
	private <T> T getObject(Class<T> type, String key, T defaultObj) {
		T obj = CollectionsUtil.getFromMapRecursive(cfg, key, type);
		return (obj != null) ? obj : defaultObj;
	}
	
	public boolean antiSignEnabled() {
		return getObject(Boolean.class, "options.anti-sign", true);
	}
	
	public void fireCommand(Subject subject, String command, String[] args) {
		lib.simulateCommand(subject, command + " " + StringsUtil.concat(args, ' '));
	}
	
	static String[] commands() {
		return COMMANDS;
	}
	
	@Override
	public void close() {
		
	}
}
