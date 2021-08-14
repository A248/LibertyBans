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

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

public abstract class SpigotCmdSender extends AbstractCmdSender<CommandSender> {

	private final JavaPlugin plugin;
	private final FactoryOfTheFuture futuresFactory;
	
	SpigotCmdSender(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
					CommandSender sender, Operator operator,
					JavaPlugin plugin, FactoryOfTheFuture futuresFactory) {
		super(formatter, audienceRepresenter, sender, operator);
		this.plugin = plugin;
		this.futuresFactory = futuresFactory;
	}
	
	@Override
	public Stream<String> getPlayersOnSameServer() {
		// Make sure not to transfer streams across threads
		return futuresFactory.supplySync(() -> {
			Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
			Collection<String> names = new HashSet<>(players.size());
			for (Player player : players) {
				names.add(player.getName());
			}
			return names;
		}).join().stream();
	}
	
	static class PlayerSender extends SpigotCmdSender {
		
		private final FactoryOfTheFuture futuresFactory;

		PlayerSender(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
					 Player player, JavaPlugin plugin, FactoryOfTheFuture futuresFactory) {
			super(formatter, audienceRepresenter,
					player, PlayerOperator.of(player.getUniqueId()), plugin, futuresFactory);
			this.futuresFactory = futuresFactory;
		}
		
		@Override
		public boolean hasPermission(String permission) {
			return futuresFactory.supplySync(() -> getRawSender().hasPermission(permission)).join();
		}
		
	}

	static class ConsoleSender extends SpigotCmdSender {

		ConsoleSender(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
					  CommandSender sender, JavaPlugin plugin, FactoryOfTheFuture futuresFactory) {
			super(formatter, audienceRepresenter,
					sender, ConsoleOperator.INSTANCE, plugin, futuresFactory);
		}
		
		@Override
		public boolean hasPermission(String permission) {
			return true;
		}
		
	}
	
}
