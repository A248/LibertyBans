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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.importing.ImportException;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.importing.PortablePunishment;
import space.arim.libertybans.env.sponge.banservice.PluginBanService;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class SpongeImportSource implements PlatformImportSource {

	private final FactoryOfTheFuture futuresFactory;
	private final ScopeManager scopeManager;
	private final Game game;

	@Inject
	public SpongeImportSource(FactoryOfTheFuture futuresFactory, ScopeManager scopeManager, Game game) {
		this.futuresFactory = futuresFactory;
		this.scopeManager = scopeManager;
		this.game = game;
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {
		BanService banService = futuresFactory.supplySync(() -> game.server().serviceProvider().banService()).join();
		if (banService instanceof PluginBanService) {
			throw new ImportException(
					"Since register-ban-service is enabled in the config.yml, it is impossible to import from Sponge. " +
							"Due to technical restrictions, you must disable register-ban-service, perform the import, " +
							"then re-enable register-ban-service if you wish to use it.");
		}
		return banService.bans().thenApply((bans) -> {
			List<PortablePunishment> punishments = new ArrayList<>(bans.size());
			for (Ban ban : bans) {
				punishments.add(mapPunishment(ban));
			}
			return punishments;
		}).join().stream();
	}

	private PortablePunishment mapPunishment(Ban ban) {

		String reason = PlainComponentSerializer.plain().serialize(
				ban.reason().orElse(Component.empty())
		);
		return new PortablePunishment(
				0,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, reason, scopeManager.globalScope(),
						ban.creationDate(), ban.expirationDate().orElse(Punishment.PERMANENT_END_DATE)
				),
				PortablePunishment.VictimInfo.simpleVictim(mapVictim(ban)),
				mapOperator(ban),
				true
		);
	}

	private Victim mapVictim(Ban ban) {
		if (ban instanceof Ban.Profile profileBan) {
			return PlayerVictim.of(profileBan.profile().uniqueId());
		} else if (ban instanceof Ban.IP ipBan) {
			return AddressVictim.of(ipBan.address());
		} else {
			throw new IllegalStateException("Unrecognized ban type: " + ban);
		}
	}

	private PortablePunishment.OperatorInfo mapOperator(Ban ban) {
		Optional<Component> source = ban.banSource();
		if (source.isEmpty()) {
			return PortablePunishment.OperatorInfo.createConsole();
		}
		String name = PlainComponentSerializer.plain().serialize(source.get());
		return PortablePunishment.OperatorInfo.createUser(null, name);
	}

}
