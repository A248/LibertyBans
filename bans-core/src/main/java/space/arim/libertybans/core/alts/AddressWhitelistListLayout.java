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

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;

public interface AddressWhitelistListLayout extends AccountListFormatting {

    @ConfDefault.DefaultStrings({"&e&oIP address &r&7at <date whitelisted> (by <staff member>)"})
    ComponentText header();

    @Override
    @ConfComments("The separator between list entries")
    @ConfDefault.DefaultString("\n")
    Component separator();

    @ConfComments({
            "How a single whitelisted IP address should be displayed. Available variables, in addition to header variables:",
            "%ADDRESS% - the IP address",
            "%DATE_ADDED% - the date the IP was added to the whitelist",
            "%DATE_ADDED_RAW% - the raw timestamp the IP was added to the whitelist",
            "%OPERATOR% - the staff member who added to the IP"
    })
    @ConfDefault.DefaultString("%ADDRESS% &r&7at %DATE_ADDED% (by %OPERATOR%)||ttp:&7Click to remove this IP address.||cmd:/libertybans ip-records whitelist remove %ADDRESS%")
    ComponentText layout();

    @ConfDefault.DefaultStrings({"&7Page &e%PAGE%&7.||ttp:Click for next page||cmd:/libertybans ip-records whitelist list %NEXTPAGE_KEY%"})
    ComponentText footer();
}
