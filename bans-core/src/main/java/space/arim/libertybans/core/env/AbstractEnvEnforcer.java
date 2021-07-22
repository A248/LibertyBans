/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.env;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;

import java.util.Objects;

public abstract class AbstractEnvEnforcer<C, P extends C> implements EnvEnforcer<P> {

	private final InternalFormatter formatter;
	private final AudienceRepresenter<C> audienceRepresenter;

	protected AbstractEnvEnforcer(InternalFormatter formatter, AudienceRepresenter<C> audienceRepresenter) {
		this.formatter = Objects.requireNonNull(formatter, "formatter");
		this.audienceRepresenter = Objects.requireNonNull(audienceRepresenter, "audienceRepresenter");
	}

	protected AudienceRepresenter<C> audienceRepresenter() {
		return audienceRepresenter;
	}
	
	@Override
	public final void sendToThoseWithPermission(String permission, ComponentLike message) {
		sendToThoseWithPermissionNoPrefix(permission, formatter.prefix(message).asComponent());
	}
	
	protected abstract void sendToThoseWithPermissionNoPrefix(String permission, Component message);

	@Override
	public void sendMessageNoPrefix(P player, ComponentLike message) {
		audienceRepresenter.toAudience(player).sendMessage(message);
	}

}
