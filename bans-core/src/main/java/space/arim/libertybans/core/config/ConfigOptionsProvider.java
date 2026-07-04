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

package space.arim.libertybans.core.config;

import jakarta.inject.Inject;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;
import space.arim.dazzleconf.ConfigurationOptions;

public final class ConfigOptionsProvider {

    private final Adventure5Compat adventure5Compat;
    private ConfigurationOptions options;

    @Inject
    public ConfigOptionsProvider(Adventure5Compat adventure5Compat) {
        this.adventure5Compat = adventure5Compat;
    }

    @SuppressWarnings("deprecation")
    public ConfigurationOptions getOptions() {
        ConfigurationOptions options = this.options;
        // Racy read fine; creation is idempotent
        if (options == null) {
            ConfigurationOptions.Builder optionsBuilder = new ConfigurationOptions.Builder()
                    .setDottedPathInConfKey(true)
                    .setCreateSingleElementCollections(true);
            ConfigSerialisers.addTo(adventure5Compat, optionsBuilder);
            options = optionsBuilder.build();
            this.options = options;
        }
        return options;
    }
}
