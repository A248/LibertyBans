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

package space.arim.libertybans.core.env;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractEnvEnforcer<P> implements EnvEnforcer<P> {

	private final FactoryOfTheFuture futuresFactory;
	private final InternalFormatter formatter;
	private final Interlocutor interlocutor;
	private final AudienceRepresenter<? super P> audienceRepresenter;

	protected AbstractEnvEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter,
								  Interlocutor interlocutor, AudienceRepresenter<? super P> audienceRepresenter) {
		this.futuresFactory = Objects.requireNonNull(futuresFactory, "futuresFactory");
		this.formatter = Objects.requireNonNull(formatter, "formatter");
		this.interlocutor = Objects.requireNonNull(interlocutor, "interlocutor");
		this.audienceRepresenter = Objects.requireNonNull(audienceRepresenter, "audienceRepresenter");
	}

	protected FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}

	protected AudienceRepresenter<? super P> audienceRepresenter() {
		return audienceRepresenter;
	}

	protected CentralisedFuture<Void> completedVoid() {
		return futuresFactory.completedFuture(null);
	}

	@Override
	public final CentralisedFuture<Void> sendToThoseWithPermission(String permission, ComponentLike message) {
		return sendToThoseWithPermissionNoPrefix(permission, formatter.prefix(message));
	}

	@Override
	public final CentralisedFuture<Void> sendToThoseWithPermissionNoPrefix(String permission, ComponentLike message) {
		return sendToThoseWithPermissionNoPrefixComponent(permission, message.asComponent());
	}

	private CentralisedFuture<Void> sendToThoseWithPermissionNoPrefixComponent(String permission, Component message) {
		Consumer<P> callback;
		if (interlocutor.shouldFilterIpAddresses()) {
			Component stripped = interlocutor.stripIpAddresses(message);
			callback = (player) -> {
				if (hasPermission(player, permission)) {
					Component chosen;
					if (hasPermission(player, Interlocutor.PERMISSION_TO_VIEW_IPS)) {
						chosen = message;
					} else {
						chosen = stripped;
					}
					sendMessageNoPrefix(player, chosen);
				}
			};
		} else {
			callback = (player) -> {
				if (hasPermission(player, permission)) {
					sendMessageNoPrefix(player, message);
				}
			};
		}
		return doForAllPlayers((players) -> players.forEach(callback));
	}

	@Override
	public final void sendMessageNoPrefix(P player, ComponentLike message) {
		audienceRepresenter.toAudience(player).sendMessage(message);
	}

	@Override
	public final CentralisedFuture<Void> enforceMatcher(TargetMatcher<P> matcher) {
		return doForAllPlayers((players) -> {
			List<P> matchedPlayers = new ArrayList<>();
			// Some platforms do not provide guarantees about concurrent iteration in presence of kicks
			// Proxies effectively must, but game server APIs like Bukkit and Sponge need not
			for (P player : players) {
				if (matcher.matches(getUniqueIdFor(player), getAddressFor(player))) {
					matchedPlayers.add(player);
				}
			}
			matchedPlayers.forEach(matcher.callback());
		});
	}

}
