/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.core.config.displayid;

import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;

import java.util.Locale;

public class AbacusForIds {

    private final Configs configs;

    @Inject
    public AbacusForIds(Configs configs) {
        this.configs = configs;
    }

    private MessagesConfig.Formatting.IdDisplay conf() {
        return configs.getMessagesConfig().formatting().idDisplay();
    }

    public String displayId(long id) {
        var conf = conf();
        Scramble algorithm = conf.scramblingAlgorithm().getImpl();
        for (int n = 0; n < conf.numberOfAlgorithmRuns(); n++) {
            id = algorithm.scramble(id);
        }
        String display = Long.toUnsignedString(id, conf.base());
        if (conf.capitalizeLettersInDisplay()) {
            display = display.toUpperCase(Locale.ROOT);
        }
        return display;
    }

    public @Nullable Long parseId(String input) {
        var conf = conf();
        long id;
        try {
            id = Long.parseUnsignedLong(input, conf.base());
        } catch (NumberFormatException ignored) {
            return null;
        }
        Scramble algorithm = conf.scramblingAlgorithm().getImpl();
        for (int n = 0; n < conf.numberOfAlgorithmRuns(); n++) {
            id = algorithm.descramble(id);
        }
        return id;
    }
}
