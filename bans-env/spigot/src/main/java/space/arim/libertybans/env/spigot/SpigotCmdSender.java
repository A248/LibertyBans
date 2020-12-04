/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.env.AbstractCmdSender;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class SpigotCmdSender extends AbstractCmdSender {

	private final FactoryOfTheFuture futuresFactory;
	private final JavaPlugin plugin;
	
	SpigotCmdSender(CmdSenderDependencies dependencies, CommandSender sender, Operator operator) {
		super(dependencies.abstractDependencies, sender, operator);
		this.futuresFactory = dependencies.futuresFactory;
		this.plugin = dependencies.plugin;
	}
	
	public static class CmdSenderDependencies {
		
		final AbstractCmdSender.AbstractDependencies abstractDependencies;
		final FactoryOfTheFuture futuresFactory;
		final JavaPlugin plugin;
		
		@Inject
		public CmdSenderDependencies(AbstractCmdSender.AbstractDependencies abstractDependencies,
				FactoryOfTheFuture futuresFactory, JavaPlugin plugin) {
			this.abstractDependencies = abstractDependencies;
			this.futuresFactory = futuresFactory;
			this.plugin = plugin;
		}
		
	}

	@Override
	public CommandSender getRawSender() {
		return (CommandSender) super.getRawSender();
	}
	
	@Override
	public Set<String> getOtherPlayersOnSameServer() {
		return futuresFactory.supplySync(() -> {
			CommandSender sender = getRawSender();
			Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
			Set<String> result = new HashSet<>(players.size());
			for (Player player : players) {
				if (sender instanceof Player && ((Player) sender).getUniqueId().equals(player.getUniqueId())) {
					continue;
				}
				result.add(player.getName());
			}
			return result;
		}).join();
	}
	
	static class PlayerSender extends SpigotCmdSender {
		
		private final FactoryOfTheFuture futuresFactory;

		PlayerSender(CmdSenderDependencies dependencies, Player player) {
			super(dependencies, player, PlayerOperator.of(player.getUniqueId()));
			this.futuresFactory = dependencies.futuresFactory;
		}
		
		@Override
		public boolean hasPermission(String permission) {
			return futuresFactory.supplySync(() -> getRawSender().hasPermission(permission)).join();
		}
		
	}

	static class ConsoleSender extends SpigotCmdSender {

		ConsoleSender(CmdSenderDependencies dependencies, CommandSender sender) {
			super(dependencies, sender, ConsoleOperator.INSTANCE);
		}
		
		@Override
		public boolean hasPermission(String permission) {
			return true;
		}
		
	}
	
}
