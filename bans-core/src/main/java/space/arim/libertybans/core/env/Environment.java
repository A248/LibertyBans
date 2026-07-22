/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

public interface Environment {

	Set<PlatformListener> createListeners(AliasCommand.RegisterOutcome registerRootCommand);

	/**
	 * Creates an alias to the plugin subcommand target
	 *
	 * @param alias the alias
	 * @param target the subcommand to alias to
	 * @return the registrable command upon success, or {@code null} if not supported by the platform
	 */
	@Nullable AliasCommand createAliasCommand(String alias, String target);

	/**
	 * Called after all aliases are registered
	 */
	void refreshServerCommands();

	/**
	 * Used for Sponge, Fabric, and the standalone application only. <br>
	 * <br>
	 * Sponge requires early command registration and service provision, Fabric needs similar command accessors;
     * while for the standalone application this usage is merely convenient.
	 *
	 * @return the platform accessors
	 */
	default Object platformAccess() {
		throw new UnsupportedOperationException("Used for Sponge/Fabric/standalone only");
	}

}
