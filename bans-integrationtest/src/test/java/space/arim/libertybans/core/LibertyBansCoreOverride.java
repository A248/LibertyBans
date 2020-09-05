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
package space.arim.libertybans.core;

import java.nio.file.Path;

import space.arim.omnibus.Omnibus;

import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.ConfigsOverride;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.it.ConfigSpec;

public class LibertyBansCoreOverride extends LibertyBansCore {

	private final Configs configs;
	
	public LibertyBansCoreOverride(Omnibus omnibus, Path folder, AbstractEnv environment, ConfigSpec spec) {
		super(omnibus, folder, environment);
		configs = new ConfigsOverride(this, spec);
	}
	
	@Override
	public Configs getConfigs() {
		return configs;
	}

}
