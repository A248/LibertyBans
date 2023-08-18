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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Singleton;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.plugin.PluginContainer;
import space.arim.api.env.AudienceRepresenter;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.sponge.SpongeAudienceRepresenter;
import space.arim.api.env.sponge.SpongePlatformHandle;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.AlwaysAvailableMuteCache;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.env.sponge.banservice.BanTypeHolder;
import space.arim.libertybans.env.sponge.plugin.PlatformAccess;

public class SpongeBindModule {

	@Singleton
	public PlatformHandle handle(PluginContainer plugin, Game game) {
		return SpongePlatformHandle.create(plugin, game);
	}

	public MuteCache muteCache(AlwaysAvailableMuteCache muteCache) {
		return muteCache;
	}

	public AudienceRepresenter<CommandCause> audienceRepresenter() {
		return new SpongeAudienceRepresenter();
	}

	public Environment environment(SpongeEnv env) {
		return env;
	}

	public EnvEnforcer<?> enforcer(SpongeEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver envUserResolver(SpongeUserResolver resolver) {
		return resolver;
	}

	public EnvMessageChannel<?> envMessageChannel(SpongeMessageChannel messageChannel) {
		return messageChannel;
	}

	public EnvServerNameDetection serverNameDetection() {
		return (scopeManager) -> {};
	}

	public PlatformImportSource platformImportSource(SpongeImportSource importSource) {
		return importSource;
	}

	public BanTypeHolder banTypeHolder(BanTypeHolder.Sponge banTypeHolder) {
		return banTypeHolder;
	}

	public PlatformAccess platformAccess(SpongePlatformAccess platformAccess) {
		return platformAccess;
	}

}
