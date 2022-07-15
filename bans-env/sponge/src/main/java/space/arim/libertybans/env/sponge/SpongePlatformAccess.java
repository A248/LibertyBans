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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.service.ban.BanService;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.env.sponge.banservice.PluginBanService;
import space.arim.libertybans.env.sponge.plugin.PlatformAccess;

public final class SpongePlatformAccess implements PlatformAccess {

	private final CommandHandler commandHandler;
	private final Configs configs;
	private final PluginBanService banService;

	@Inject
	public SpongePlatformAccess(CommandHandler commandHandler, Configs configs, PluginBanService banService) {
		this.commandHandler = commandHandler;
		this.configs = configs;
		this.banService = banService;
	}

	@Override
	public Command.Raw commandHandler() {
		return commandHandler;
	}

	@Override
	public boolean registerBanService() {
		return configs.getMainConfig().platforms().sponge().registerBanService();
	}

	@Override
	public BanService banService() {
		return banService;
	}

}
