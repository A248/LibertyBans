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

package space.arim.libertybans.api;

import space.arim.libertybans.api.user.AccountSupervisor;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.api.formatter.PunishmentFormatter;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.user.UserResolver;

/**
 * The main entry point to the LibertyBans API
 * 
 * @author A248
 *
 */
public interface LibertyBans {

	/**
	 * Gets the {@link Omnibus} instance used for firing events and registering the
	 * instance
	 * 
	 * @return the omnibus instance
	 */
	Omnibus getOmnibus();

	/**
	 * Gets the {@link FactoryOfTheFuture} used to create futures
	 * 
	 * @return the factory of the future used
	 */
	FactoryOfTheFuture getFuturesFactory();

	/**
	 * Gets the punishment drafter used for creating punishments
	 * 
	 * @return the punishment drafter
	 */
	PunishmentDrafter getDrafter();

	/**
	 * Gets the punishment revoker used for undoing punishments
	 * 
	 * @return the punishment revoker
	 */
	PunishmentRevoker getRevoker();

	/**
	 * Gets the punishment selector used to select punishments from the database
	 * 
	 * @return the punishment selector
	 */
	PunishmentSelector getSelector();

	/**
	 * Gets the punishment database. Per the class javadoc of
	 * {@link PunishmentDatabase}, use of this method should be avoided in all but a
	 * few cases
	 * 
	 * @return the punishment database
	 */
	PunishmentDatabase getDatabase();

	/**
	 * Gets the punishment formatter
	 * 
	 * @return the formatter manager
	 */
	PunishmentFormatter getFormatter();

	/**
	 * Gets the scope manager used to create scopes
	 * 
	 * @return the scope manager
	 */
	ScopeManager getScopeManager();

	/**
	 * Gets the user resolver for looking up UUIDs and names
	 * 
	 * @return the user resolver
	 */
	UserResolver getUserResolver();

	/**
	 * Gets the account supervisor for alt detection and management
	 *
	 * @return the account supervisor
	 */
	AccountSupervisor getAccountSupervisor();

}
