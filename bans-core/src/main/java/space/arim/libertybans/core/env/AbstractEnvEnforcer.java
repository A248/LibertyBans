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

package space.arim.libertybans.core.env;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Objects;

public abstract class AbstractEnvEnforcer<P> implements EnvEnforcer<P> {

	private final FactoryOfTheFuture futuresFactory;
	private final InternalFormatter formatter;
	private final AudienceRepresenter<? super P> audienceRepresenter;

	protected AbstractEnvEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter,
								  AudienceRepresenter<? super P> audienceRepresenter) {
		this.futuresFactory = Objects.requireNonNull(futuresFactory, "futuresFactory");
		this.formatter = Objects.requireNonNull(formatter, "formatter");
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
		return sendToThoseWithPermissionNoPrefix(permission, formatter.prefix(message).asComponent());
	}
	
	protected abstract CentralisedFuture<Void> sendToThoseWithPermissionNoPrefix(String permission, Component message);

	@Override
	public void sendMessageNoPrefix(P player, ComponentLike message) {
		audienceRepresenter.toAudience(player).sendMessage(message);
	}

}
