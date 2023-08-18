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

package space.arim.libertybans.core.commands.extra;

import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionPredicate;

public interface ParseScope<R> {

	R explicitScope(ServerScope scope);

	R defaultValue(ScopeManager scopeManager);

	static ParseScope<ServerScope> fallbackToDefaultPunishingScope() {
		return new ParseScope<>() {
			@Override
			public ServerScope explicitScope(ServerScope scope) {
				return scope;
			}

			@Override
			public ServerScope defaultValue(ScopeManager scopeManager) {
				return scopeManager.defaultPunishingScope();
			}
		};
	}

	static ParseScope<SelectionPredicate<ServerScope>> selectionPredicate() {
		return new ParseScope<>() {
			@Override
			public SelectionPredicate<ServerScope> explicitScope(ServerScope scope) {
				return SelectionPredicate.matchingOnly(scope);
			}

			@Override
			public SelectionPredicate<ServerScope> defaultValue(ScopeManager scopeManager) {
				return SelectionPredicate.matchingAll();
			}
		};
	}

}
