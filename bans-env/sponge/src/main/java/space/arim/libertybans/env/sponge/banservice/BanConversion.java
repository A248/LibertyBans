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

package space.arim.libertybans.env.sponge.banservice;

import jakarta.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.Ban;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.net.InetAddress;
import java.util.UUID;

public final class BanConversion {

	private final InternalFormatter formatter;
	private final Game game;
	private final BanTypeHolder banTypeHolder;

	@Inject
	public BanConversion(InternalFormatter formatter, Game game, BanTypeHolder banTypeHolder) {
		this.formatter = formatter;
		this.game = game;
		this.banTypeHolder = banTypeHolder;
	}

	private GameProfile createGameProfile(UUID uuid) {
		return game.factoryProvider().provide(GameProfile.Factory.class).of(uuid, null);
	}

	CentralisedFuture<Ban> toSpongeBan(Punishment punishment) {
		assert punishment.getType() == PunishmentType.BAN;
		return formatter.formatOperator(punishment.getOperator()).thenApply((formattedOperator) -> {

			Victim victim = punishment.getVictim();
			return switch (victim.getType()) {
			case PLAYER -> {
				GameProfile profile = createGameProfile(((PlayerVictim) victim).getUUID());
				yield new PunishmentAsBan.WithProfile(punishment, banTypeHolder, formattedOperator, profile);
			}
			case ADDRESS -> {
				InetAddress address = ((AddressVictim) victim).getAddress().toInetAddress();
				yield new PunishmentAsBan.WithIP(punishment, banTypeHolder, formattedOperator, address);
			}
			case COMPOSITE -> {
				InetAddress address = ((CompositeVictim) victim).getAddress().toInetAddress();
				yield new PunishmentAsBan.WithIP(punishment, banTypeHolder, formattedOperator, address);
			}
			};
		});
	}

	Victim extractVictim(Ban ban) {
		if (ban instanceof PunishmentAsBan punishmentAsBan) {
			return punishmentAsBan.underlyingPunishment().getVictim();
		}
		if (ban instanceof Ban.Profile profileBan) {
			return PlayerVictim.of(profileBan.profile().uniqueId());
		} else if (ban instanceof Ban.IP ipBan) {
			return AddressVictim.of(ipBan.address());
		} else {
			throw new IllegalArgumentException("Unknown ban victim for " + ban);
		}
	}

}
