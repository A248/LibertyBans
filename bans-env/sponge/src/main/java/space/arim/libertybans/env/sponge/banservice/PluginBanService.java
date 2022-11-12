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
import jakarta.inject.Singleton;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
public final class PluginBanService implements BanService {

	private final FactoryOfTheFuture futuresFactory;
	private final PunishmentRevoker revoker;
	private final PunishmentSelector selector;
	private final BanConversion conversion;

	@Inject
	public PluginBanService(FactoryOfTheFuture futuresFactory, PunishmentRevoker revoker,
							PunishmentSelector selector, BanConversion conversion) {
		this.futuresFactory = futuresFactory;
		this.revoker = revoker;
		this.selector = selector;
		this.conversion = conversion;
	}

	private <B extends Ban> CompletableFuture<Collection<B>> selectCertainBans(
			Set<Victim.VictimType> victimTypesAllowed, Function<Ban, B> castingFunction) {
		return selector.selectionBuilder()
				.type(PunishmentType.BAN)
				.victimTypes(SelectionPredicate.matchingAnyOf(victimTypesAllowed))
				.build()
				.getAllSpecificPunishments()
				.toCompletableFuture()
				.thenCompose((bans) -> {
					List<CentralisedFuture<Ban>> spongeBans = new ArrayList<>(bans.size());
					for (Punishment ban : bans) {
						assert victimTypesAllowed.contains(ban.getVictim().getType());
						spongeBans.add(conversion.toSpongeBan(ban));
					}
					// Convert list of futures to future of list
					return futuresFactory.allOf(spongeBans).thenApply((ignore) -> {
						List<B> finishedBans = new ArrayList<>(spongeBans.size());
						for (CentralisedFuture<Ban> spongeBan : spongeBans) {
							B castedBan = castingFunction.apply(spongeBan.join());
							finishedBans.add(castedBan);
						}
						return Collections.unmodifiableList(finishedBans);
					});
				});
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompletableFuture<Collection<? extends Ban>> bans() {
		CompletableFuture<?> blameGenerics = selectCertainBans(
				Set.of(Victim.VictimType.values()), Function.identity()
		);
		return (CompletableFuture<Collection<? extends Ban>>) blameGenerics;
	}

	@Override
	public CompletableFuture<Collection<Ban.Profile>> profileBans() {
		return selectCertainBans(
				Set.of(Victim.VictimType.PLAYER), (ban) -> (Ban.Profile) ban
		);
	}

	@Override
	public CompletableFuture<Collection<Ban.IP>> ipBans() {
		return selectCertainBans(
				Set.of(Victim.VictimType.ADDRESS, Victim.VictimType.COMPOSITE), (ban) -> (Ban.IP) ban
		);
	}

	@Override
	public CompletableFuture<Optional<Ban.Profile>> find(GameProfile profile) {
		return selector.selectionBuilder()
				.type(PunishmentType.BAN)
				.victim(PlayerVictim.of(profile.uniqueId()))
				.build()
				.getFirstSpecificPunishment()
				.thenCompose((optPunishment) -> {
					if (optPunishment.isEmpty()) {
						return futuresFactory.completedFuture(Optional.<Ban.Profile>empty());
					}
					return conversion.toSpongeBan(optPunishment.get())
							.thenApply((spongeBan) -> Optional.of((Ban.Profile) spongeBan));
				})
				.toCompletableFuture();
	}

	@Override
	public CompletableFuture<Optional<Ban.IP>> find(InetAddress address) {
		return selector.selectionBuilder()
				.type(PunishmentType.BAN)
				.victims(SelectionPredicate.matchingAnyOf(
						AddressVictim.of(address), CompositeVictim.of(CompositeVictim.WILDCARD_UUID, address)
				))
				.build()
				.getFirstSpecificPunishment()
				.thenCompose((optPunishment) -> {
					if (optPunishment.isEmpty()) {
						return futuresFactory.completedFuture(Optional.<Ban.IP>empty());
					}
					return conversion.toSpongeBan(optPunishment.get())
							.thenApply((spongeBan) -> Optional.of((Ban.IP) spongeBan));
				})
				.toCompletableFuture();
	}

	@Override
	public CompletableFuture<Boolean> pardon(GameProfile profile) {
		return revoker
				.revokeByTypeAndVictim(PunishmentType.BAN, PlayerVictim.of(profile.uniqueId()))
				.undoPunishment()
				.toCompletableFuture();
	}

	@Override
	public CompletableFuture<Boolean> pardon(InetAddress address) {
		List<Victim> victims = List.of(
				AddressVictim.of(address), CompositeVictim.of(CompositeVictim.WILDCARD_UUID, address)
		);
		return revoker
				.revokeByTypeAndPossibleVictims(PunishmentType.BAN, victims)
				.undoPunishment()
				.toCompletableFuture();
	}

	@Override
	public CompletableFuture<Boolean> remove(Ban ban) {
		if (ban instanceof PunishmentAsBan punishmentAsBan) {
			return punishmentAsBan.underlyingPunishment()
					.undoPunishment()
					.toCompletableFuture();
		}
		return selector.selectionBuilder()
				.type(PunishmentType.BAN)
				.victim(conversion.extractVictim(ban))
				.build()
				.getFirstSpecificPunishment()
				.thenCompose((optPunishment) -> {
					if (optPunishment.isEmpty()) {
						return futuresFactory.completedFuture(false);
					}
					Punishment punishment = optPunishment.get();
					if (!punishment.getStartDate().equals(ban.creationDate())) {
						return futuresFactory.completedFuture(false);
					}
					// For our purposes, this must be the same punishment
					// Note that punishment reason and end time might reasonably change
					return punishment.undoPunishment();
				})
				.toCompletableFuture();
	}

	@Override
	public CompletableFuture<Optional<? extends Ban>> add(Ban ban) {
		throw new UnsupportedOperationException(
				"Cannot add bans via the BanService implementation. Unfortunately, it is simply not possible " +
						"for LibertyBans to implement this method. A Sponge Ban does not have the necessary details.");
	}

}
