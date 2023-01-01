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

import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;
import space.arim.api.env.AudienceRepresenter;
import space.arim.api.env.sponge.SpongeAudienceRepresenter;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractCmdSender;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

public abstract class SpongeCmdSender<C extends Subject> extends AbstractCmdSender<C> {

	private final Game game;
	private final FactoryOfTheFuture futuresFactory;

	SpongeCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
					AudienceRepresenter<C> audienceRepresenter,
					C rawSender, Operator operator, Game game, FactoryOfTheFuture futuresFactory) {
		super(formatter, interlocutor, audienceRepresenter, rawSender, operator);
		this.game = game;
		this.futuresFactory = futuresFactory;
	}

	@Override
	public final Stream<String> getPlayerNames() {
		// Make sure not to transfer streams across threads
		return futuresFactory.supplySync(() -> {
			Collection<ServerPlayer> players = game.server().onlinePlayers();
			Collection<String> names = new HashSet<>(players.size());
			for (ServerPlayer player : players) {
				names.add(player.name());
			}
			return names;
		}).join().stream();
	}

	@Override
	public final Stream<String> getPlayerNamesOnSameServer() {
		return getPlayerNames();
	}

	@Override
	public final boolean hasPermission(String permission) {
		// The PermissionService is required to be thread-safe
		return getRawSender().hasPermission(permission);
	}

	static class PlayerCmdSender extends SpongeCmdSender<ServerPlayer> {

		PlayerCmdSender(InternalFormatter formatter, Interlocutor interlocutor,
						ServerPlayer player, Game game, FactoryOfTheFuture futuresFactory) {
			super(formatter, interlocutor, AudienceRepresenter.identity(),
					player, PlayerOperator.of(player.uniqueId()), game, futuresFactory);
		}

	}

	static class ConsoleSender extends SpongeCmdSender<CommandCause> {

		ConsoleSender(InternalFormatter formatter, Interlocutor interlocutor,
					  CommandCause sender, Game game, FactoryOfTheFuture futuresFactory) {
			super(formatter, interlocutor, new SpongeAudienceRepresenter(),
					sender, ConsoleOperator.INSTANCE, game, futuresFactory);
		}

	}

}
