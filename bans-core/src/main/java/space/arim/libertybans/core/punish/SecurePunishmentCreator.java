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
package space.arim.libertybans.core.punish;

import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.selector.MuteCache;

@Singleton
public class SecurePunishmentCreator implements PunishmentCreator {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<Enforcer> enforcer;
	private final Provider<InternalRevoker> revoker;
	private final Provider<MuteCache> muteCache;

	@Inject
	public SecurePunishmentCreator(FactoryOfTheFuture futuresFactory, Provider<Enforcer> enforcer,
			Provider<InternalRevoker> revoker, Provider<MuteCache> muteCache) {
		this.futuresFactory = futuresFactory;
		this.enforcer = enforcer;
		this.revoker = revoker;
		this.muteCache = muteCache;
	}

	FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}

	Enforcer enforcer() {
		return enforcer.get();
	}

	InternalRevoker revoker() {
		return revoker.get();
	}

	MuteCache muteCache() {
		return muteCache.get();
	}

	@Override
	public Punishment createPunishment(int id, PunishmentType type, Victim victim, Operator operator, String reason,
			ServerScope scope, long start, long end) {
		Instant startDate = Instant.ofEpochSecond(start);
		Instant endDate = (end == 0L) ? Instant.MAX : Instant.ofEpochSecond(end);
		return new SecurePunishment(this, id, type, victim, operator, reason, scope, startDate, endDate);
	}

}
