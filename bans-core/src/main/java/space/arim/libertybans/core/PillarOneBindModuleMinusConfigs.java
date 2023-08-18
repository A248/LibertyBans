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

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.addon.StandardAddonCenter;
import space.arim.libertybans.core.punish.GlobalEnforcement;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.core.selector.IntelligentGuardian;
import space.arim.libertybans.core.punish.InternalRevoker;
import space.arim.libertybans.core.punish.LocalEnforcer;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.punish.Revoker;
import space.arim.libertybans.core.punish.SecurePunishmentCreator;
import space.arim.libertybans.core.punish.StandardGlobalEnforcement;
import space.arim.libertybans.core.punish.StandardLocalEnforcer;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.scope.StandardScopeManager;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.selector.SelectorImpl;

public abstract class PillarOneBindModuleMinusConfigs {

	public BaseFoundation foundation(LifecycleGodfather godfather) {
		return godfather;
	}

	public PunishmentCreator creator(SecurePunishmentCreator creator) {
		return creator;
	}

	public InternalRevoker revoker(Revoker revoker) {
		return revoker;
	}

	public GlobalEnforcement enforcement(StandardGlobalEnforcement enforcement) {
		return enforcement;
	}

	public LocalEnforcer enforcer(StandardLocalEnforcer<?> enforcer) {
		return enforcer;
	}

	public Guardian guardian(IntelligentGuardian guardian) {
		return guardian;
	}

	public InternalSelector selector(SelectorImpl selector) {
		return selector;
	}

	public InternalScopeManager scopeManager(StandardScopeManager scopeManager) {
		return scopeManager;
	}

	public AddonCenter addonCenter(StandardAddonCenter addonCenter) {
		return addonCenter;
	}
}
