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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.api.env.AudienceRepresenter;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.bukkit.BukkitAudienceRepresenter;
import space.arim.api.env.bukkit.BukkitPlatformHandle;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.AlwaysAvailableMuteCache;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.adventure.MorePaperLibAdventure;

public class SpigotBindModule {

	@Singleton
	public PlatformHandle handle(JavaPlugin plugin) {
		return BukkitPlatformHandle.create(plugin);
	}

	public MuteCache muteCache(AlwaysAvailableMuteCache muteCache) {
		return muteCache;
	}

	public AudienceRepresenter<CommandSender> audienceRepresenter() {
		return new BukkitAudienceRepresenter();
	}

	public Environment environment(SpigotEnv env) {
		return env;
	}

	public EnvEnforcer<?> enforcer(SpigotEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(SpigotUserResolver resolver) {
		return resolver;
	}

	public EnvMessageChannel<?> messageChannel(SpigotMessageChannel messageChannel) {
		return messageChannel;
	}

	@Singleton
	public CommandMapHelper commandMapHelper(SimpleCommandMapHelper scmh) {
		return new CachingCommandMapHelper(scmh);
	}

	@Singleton
	public MorePaperLib morePaperLib(JavaPlugin plugin) {
		return new MorePaperLib(plugin);
	}

	@Singleton
	public MorePaperLibAdventure morePaperLibAdventure(MorePaperLib morePaperLib) {
		return new MorePaperLibAdventure(morePaperLib);
	}

	public EnvServerNameDetection serverNameDetection() {
		return (scopeManager) -> {};
	}

	public PlatformImportSource platformImportSource(BukkitImportSource importSource) {
		return importSource;
	}

}
