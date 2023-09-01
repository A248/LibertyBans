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

package space.arim.libertybans.core;

import jakarta.inject.Singleton;

import space.arim.libertybans.api.user.AccountSupervisor;
import space.arim.libertybans.core.alts.Supervisor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.user.UserResolver;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.punish.Enactor;
import space.arim.libertybans.core.punish.InternalRevoker;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.service.AsynchronicityManager;
import space.arim.libertybans.core.service.LateBindingFactoryOfTheFuture;
import space.arim.libertybans.core.uuid.UUIDManager;

public class ApiBindModule {

	public LibertyBans libertyBansApi(LibertyBansApi api) {
		return api;
	}

	@Singleton
	public FactoryOfTheFuture futuresFactory(AsynchronicityManager asyncManager) {
		return new LateBindingFactoryOfTheFuture(asyncManager);
	}

	public PunishmentDatabase database(DatabaseManager databaseManager) {
		return databaseManager.getExternal();
	}

	public PunishmentDrafter drafter(Enactor enactor) {
		return enactor;
	}

	public PunishmentRevoker revoker(InternalRevoker revoker) {
		return revoker;
	}

	public PunishmentSelector selector(InternalSelector selector) {
		return selector;
	}

	public PunishmentFormatter formatter(InternalFormatter formatter) {
		return formatter;
	}

	public ScopeManager scopeManager(InternalScopeManager scopeManager) {
		return scopeManager;
	}

	public UserResolver userResolver(UUIDManager uuidManager) {
		return uuidManager;
	}

	public AccountSupervisor accountSupervisor(Supervisor supervisor) {
		return supervisor;
	}

}
