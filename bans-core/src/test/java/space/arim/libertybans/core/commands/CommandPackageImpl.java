/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public interface CommandPackageImpl {

	CommandPackage create(String args);

	CommandPackage createEmpty();

	class Provider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			CommandPackageImpl arrayCommandPackage = new CommandPackageImpl() {
				@Override
				public CommandPackage create(String args) {
					return ArrayCommandPackage.create(args.split(" "));
				}

				@Override
				public CommandPackage createEmpty() {
					return ArrayCommandPackage.create();
				}
			};
			CommandPackageImpl stringCommandPackage = new CommandPackageImpl() {
				@Override
				public CommandPackage create(String args) {
					return StringCommandPackage.create(args);
				}

				@Override
				public CommandPackage createEmpty() {
					return StringCommandPackage.create("");
				}
			};
			return Stream.of(arrayCommandPackage, stringCommandPackage).map(Arguments::of);
		}
	}

}
