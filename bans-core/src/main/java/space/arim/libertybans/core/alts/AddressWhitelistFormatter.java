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

package space.arim.libertybans.core.alts;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.jsonchat.adventure.util.Adventure5Compat;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.pagination.InstantThenAddress;
import space.arim.libertybans.core.database.pagination.KeysetPage;

import java.util.Objects;

public class AddressWhitelistFormatter {

    private final Configs configs;
    private final InternalFormatter formatter;
    private final Adventure5Compat adventure5Compat;

    @Inject
    public AddressWhitelistFormatter(Configs configs, InternalFormatter formatter, Adventure5Compat adventure5Compat) {
        this.configs = configs;
        this.formatter = formatter;
        this.adventure5Compat = adventure5Compat;
    }

    public Component formatMessage(KeysetPage<AddressWhitelist.Entry, InstantThenAddress> response, int page) {
        var listingConf = configs.getMessagesConfig().ipRecords().whitelist().list().layout();
        return new FormatAccounts<>(
                adventure5Compat,
                listingConf,
                response
        ).format(null, page, new EntryFormat(configs, listingConf, formatter));
    }

    private record EntryFormat(Configs configs, AddressWhitelistListLayout listingConf, InternalFormatter formatter) implements FormatAccounts.ElementFormat<AddressWhitelist.Entry> {

        @Override
        public ComponentLike format(@Nullable String target, AddressWhitelist.Entry entry) {
            Operator operator = entry.operator();
            String operatorDisplay;
            if (operator instanceof PlayerOperator) {
                operatorDisplay = Objects.requireNonNullElse(entry.operatorName(), configs.getMessagesConfig().formatting().victimDisplay().playerNameUnknown());
            } else {
                operatorDisplay = configs.getMessagesConfig().formatting().consoleDisplay();
            }
            return listingConf.layout()
                    .replaceText("%ADDRESS%", entry.address().toString())
                    .replaceText("%OPERATOR%", operatorDisplay)
                    .replaceText("%DATE_ADDED%", formatter.formatAbsoluteDate(entry.date()))
                    .replaceText("%DATE_ADDED_RAW%", Long.toString(entry.date().getEpochSecond()));
        }
    }
}
