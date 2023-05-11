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

package space.arim.libertybans.api.punish;

import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Victim;

public interface DraftSanctionBuilder<B extends DraftSanctionBuilder<B, D>, D extends DraftSanction> {

	/**
	 * Sets the victim of this builder to the specified one. Required operation. <br>
	 * <br>
	 * If the victim is a {@code CompositeVictim}, it cannot use either {@link CompositeVictim#WILDCARD_UUID}
	 * or {@link CompositeVictim#WILDCARD_ADDRESS}
	 *
	 * @param victim the victim of the punishmnt
	 * @return this builder
	 * @throws IllegalArgumentException if the victim is a {@code CompositeVictim} and uses either
	 * {@code WILDCARD_UUID} or {@code WILDCARD_ADDRESS}
	 */
	B victim(Victim victim);

	/**
	 * Sets the operator of this builder to the specified one. If unspecified, the
	 * console operator is used ({@link ConsoleOperator#INSTANCE})
	 *
	 * @param operator the operator of the punishment, by default the console
	 * @return this builder
	 */
	B operator(Operator operator);

	/**
	 * Builds into a full draft sanction. All the required information must be set on this builder. <br>
	 * <br>
	 * May be used repeatedly without side effects.
	 *
	 * @return a draft sanction from this builder's details
	 * @throws IllegalStateException    if any of the required details on this
	 *                                  builder are not set
	 */
	D build();

}
