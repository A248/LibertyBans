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
package space.arim.libertybans.core.config;

import java.nio.file.Path;

import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.it.ConfigSpec;

public class ConfigsOverride extends Configs {

	private final ConfigSpec spec;
	
	public ConfigsOverride(Path folder, ConfigSpec spec) {
		super(folder);
		this.spec = spec;
	}
	
	@Override
	public SqlConfig getSqlConfig() {
		return new Delegator<>(SqlConfig.class, super.getSqlConfig()) {
			@Override
			Object replacementFor(SqlConfig original, String methodName) {
				if (methodName.equals("vendor")) {
					return spec.getVendor();
				}
				if (methodName.equals("authDetails")) {
					return new SqlConfig.AuthDetails() {
						@Override
						public String host() {
							return "localhost";
						}
						@Override
						public int port() {
							return spec.getPort();
						}
						@Override
						public String database() {
							return spec.getDatabase();
						}
						@Override
						public String username() {
							return "root";
						}
						@Override
						public String password() {
							return "";
						}
					};
				}
				return null;
			}
		}.proxy();
	}
	
	@Override
	public MainConfig getMainConfig() {
		return new Delegator<>(MainConfig.class, super.getMainConfig()) {
			@Override
			Object replacementFor(MainConfig original, String methodName) {
				if (methodName.equals("enforcement")) {
					return enforcement(original);
				}
				return null;
			}
			private EnforcementConfig enforcement(MainConfig original) {
				return new Delegator<>(EnforcementConfig.class, original.enforcement()) {
					@Override
					Object replacementFor(EnforcementConfig original, String methodName) {
						if (methodName.equals("addressStrictness")) {
							return spec.getAddressStrictness();
						}
						return null;
					}
				}.proxy();
			}
			
		}.proxy();
	}

}
