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

import net.kyori.adventure.text.Component;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanType;
import space.arim.libertybans.api.punish.Punishment;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;

abstract class PunishmentAsBan implements Ban {

	private final Punishment punishment;
	private final BanType banType;
	private final String formattedOperator;

	PunishmentAsBan(Punishment punishment, BanType banType, String formattedOperator) {
		this.punishment = punishment;
		this.banType = banType;
		this.formattedOperator = formattedOperator;
	}

	Punishment underlyingPunishment() {
		return punishment;
	}

	@Override
	public BanType type() {
		return banType;
	}

	@Override
	public Optional<Component> reason() {
		return Optional.of(Component.text(punishment.getReason()));
	}

	@Override
	public Instant creationDate() {
		return punishment.getStartDate();
	}

	@Override
	public Optional<Component> banSource() {
		return Optional.of(Component.text(formattedOperator));
	}

	@Override
	public Optional<Instant> expirationDate() {
		if (punishment.isPermanent()) {
			return Optional.empty();
		}
		return Optional.of(punishment.getEndDate());
	}

	@Override
	public boolean isIndefinite() {
		return punishment.isPermanent();
	}


	static final class WithProfile extends PunishmentAsBan implements Ban.Profile {

		private final GameProfile profile;

		WithProfile(Punishment punishment, BanTypeHolder banTypeHolder, String formattedOperator, GameProfile profile) {
			super(punishment, banTypeHolder.profile(), formattedOperator);
			this.profile = profile;
		}

		@Override
		public GameProfile profile() {
			return profile;
		}

	}

	static final class WithIP extends PunishmentAsBan implements Ban.IP {

		private final InetAddress address;

		WithIP(Punishment punishment, BanTypeHolder banTypeHolder, String formattedOperator, InetAddress address) {
			super(punishment, banTypeHolder.ip(), formattedOperator);
			this.address = address;
		}

		@Override
		public InetAddress address() {
			return address;
		}

	}
}
