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

package space.arim.libertybans.core.commands;

import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CommandArgMatch implements ArgumentMatcher<CommandPackage> {

    private final String[] expected;

    public CommandArgMatch(String...expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(CommandPackage argument) {
        List<String> collect = new ArrayList<>();
        while (argument.hasNext()) {
            collect.add(argument.next());
        }
        return Arrays.asList(expected).equals(collect);
    }
}
