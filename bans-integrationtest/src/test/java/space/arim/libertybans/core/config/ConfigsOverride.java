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

import java.util.ArrayList;
import java.util.List;

import space.arim.api.configure.SingleKeyValueTransformer;
import space.arim.api.configure.ValueTransformer;

import space.arim.libertybans.core.LibertyBansCoreOverride;
import space.arim.libertybans.it.ConfigSpec;

public class ConfigsOverride extends Configs {

	private final ConfigSpec spec;
	
	public ConfigsOverride(LibertyBansCoreOverride core, ConfigSpec spec) {
		super(core);
		this.spec = spec;
	}
	
	@Override
	List<ValueTransformer> sqlValueTransformers() {
		return prepend(super.sqlValueTransformers(),
				SingleKeyValueTransformer.create("rdms-vendor", (oldVendor) -> spec.getVendor()),
				SingleKeyValueTransformer.create("auth-details.port", (oldPort) -> spec.getPort()),
				SingleKeyValueTransformer.create("auth-details.user", (oldUser) -> "root"),
				SingleKeyValueTransformer.create("auth-details.password", (oldPass) -> ""),
				SingleKeyValueTransformer.create("auth-details.database", (oldDb) -> spec.getDatabase()));
	}
	
	@Override
	List<ValueTransformer> configValueTransformers() {
		var replacement = SingleKeyValueTransformer.create("enforcement.address-strictness",
				(oldStrictness) -> spec.getAddressStrictness());
		return prepend(super.configValueTransformers(), replacement);
	}
	
	private static List<ValueTransformer> prepend(List<ValueTransformer> transformers, ValueTransformer...elements) {
		List<ValueTransformer> result = new ArrayList<>(transformers);
		for (ValueTransformer prepend : elements) {
			result.add(0, prepend);
		}
		return List.copyOf(result);
	}

}
