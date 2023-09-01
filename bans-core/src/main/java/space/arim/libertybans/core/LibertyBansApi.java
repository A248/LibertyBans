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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.user.AccountSupervisor;
import space.arim.libertybans.api.user.UserResolver;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

public class LibertyBansApi implements LibertyBans {

	private final Omnibus omnibus;
	private final FactoryOfTheFuture futuresFactory;
	private final PunishmentDrafter drafter;
	private final PunishmentRevoker revoker;
	private final PunishmentSelector selector;
	private final Provider<PunishmentDatabase> dbProvider;
	private final PunishmentFormatter formatter;
	private final ScopeManager scopeManager;
	private final UserResolver userResolver;
	private final AccountSupervisor accountSupervisor;

	@Inject
	public LibertyBansApi(Omnibus omnibus, FactoryOfTheFuture futuresFactory, Provider<PunishmentDatabase> dbProvider,
						  PunishmentDrafter drafter, PunishmentRevoker revoker, PunishmentSelector selector,
						  PunishmentFormatter formatter, ScopeManager scopeManager,
						  UserResolver userResolver, AccountSupervisor accountSupervisor) {
		this.omnibus = omnibus;
		this.futuresFactory = futuresFactory;
		this.drafter = drafter;
		this.revoker = revoker;
		this.selector = selector;
		this.dbProvider = dbProvider;
		this.formatter = formatter;
		this.scopeManager = scopeManager;
		this.userResolver = userResolver;
		this.accountSupervisor = accountSupervisor;
	}

	@Override
	public Omnibus getOmnibus() {
		return omnibus;
	}

	@Override
	public FactoryOfTheFuture getFuturesFactory() {
		return futuresFactory;
	}

	@Override
	public PunishmentDrafter getDrafter() {
		return drafter;
	}

	@Override
	public PunishmentRevoker getRevoker() {
		return revoker;
	}

	@Override
	public PunishmentSelector getSelector() {
		return selector;
	}

	@Override
	public PunishmentDatabase getDatabase() {
		return dbProvider.get();
	}

	@Override
	public PunishmentFormatter getFormatter() {
		return formatter;
	}

	@Override
	public ScopeManager getScopeManager() {
		return scopeManager;
	}

	@Override
	public UserResolver getUserResolver() {
		return userResolver;
	}

	@Override
	public AccountSupervisor getAccountSupervisor() {
		return accountSupervisor;
	}

}
