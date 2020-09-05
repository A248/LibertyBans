/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.test.applicable;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.LibertyBansCore;

abstract class ApplicabilityTestingBase {

	final LibertyBansCore core;
	final PunishmentType type;
	final Operator operator;
	final String info;
	
	ApplicabilityTestingBase(LibertyBansCore core, PunishmentType type, Operator operator) {
		this.core = core;
		this.type = type;
		this.operator = operator;
		info = type + "/" + core.getDatabase().getVendor() + '/' + core.getConfigs().getAddressStrictness();
	}
	
	public abstract void doTest();
	
}
