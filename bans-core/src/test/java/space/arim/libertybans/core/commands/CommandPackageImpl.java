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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.stream.Stream;

public interface CommandPackageImpl {

	CommandPackage create(String args);

	CommandPackage createEmpty();

	class Provider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
															ExtensionContext context) throws Exception {
			CommandPackageImpl impl1 = new CommandPackageImpl() {
				@Override
				public CommandPackage create(String args) {
					return ArrayCommandPackage.create(args.split(" ", -1));
				}

				@Override
				public CommandPackage createEmpty() {
					return ArrayCommandPackage.create();
				}
			};
			CommandPackageImpl impl2 = new CommandPackageImpl() {
				@Override
				public CommandPackage create(String args) {
					return StringCommandPackage.create(args);
				}

				@Override
				public CommandPackage createEmpty() {
					CommandPackage commandPackage = StringCommandPackage.create("");
					commandPackage.next();
					return commandPackage;
				}
			};
			class Impl3 implements CommandPackageImpl {

				private final CommandPackageImpl inner;

                Impl3(CommandPackageImpl inner) {
                    this.inner = inner;
                }

				@Override
				public CommandPackage create(String args) {
					int indexOfSpace = args.indexOf(' ');
					if (indexOfSpace == -1) {
						return inner.create(args);
					} else {
						return new PrependedCommandPackage(
								args.substring(0, indexOfSpace),
								inner.create(args.substring(indexOfSpace + 1))
						);
					}
				}

				@Override
				public CommandPackage createEmpty() {
					return inner.createEmpty();
				}
			}
			return Stream.of(impl1, impl2, new Impl3(impl1), new Impl3(impl2)).map(Arguments::of);
		}
	}

}
