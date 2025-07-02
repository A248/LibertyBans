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

package space.arim.libertybans.core.alts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.user.AccountBase;
import space.arim.libertybans.core.database.pagination.KeysetPage;

import java.util.ArrayList;
import java.util.List;

record FormatAccounts<A extends AccountBase, F>(AccountListFormatting config, KeysetPage<A, F> response) {

    Component format(String target, int page, ElementFormat<A> elementFormat) {
        // Construct the form of the message
        ComponentText built;
        {
            List<A> data = response.data();
            List<ComponentLike> messages = new ArrayList<>(data.size() + 4);

            ComponentText header = config.header();
            if (!header.isEmpty()) {
                messages.add(header.replaceText("%TARGET%", target));
                messages.add(Component.newline());
            }
            for (int n = 0; n < data.size(); n++) {
                if (n != 0) {
                    Component separator = config.separator();
                    if (separator.equals(Component.empty())) {
                        messages.add(Component.newline());
                    } else {
                        messages.add(separator);
                    }
                }
                messages.add(elementFormat.format(target, data.get(n)));
            }
            ComponentText footer = config.footer();
            if (!footer.isEmpty()) {
                messages.add(Component.newline());
                messages.add(footer);
            }
            Component concat = TextComponent.ofChildren(messages.toArray(ComponentLike[]::new));
            built = ComponentText.create(concat);
        }
        // Add in the variable content
        built = built.replaceText("%TARGET%", target);
        if (page != -1) {
            built = built.replaceText(response.new VariableReplacer(page));
        }
        return built.asComponent();
    }

    interface ElementFormat<T> {
        ComponentLike format(String target, T element);
    }

}
