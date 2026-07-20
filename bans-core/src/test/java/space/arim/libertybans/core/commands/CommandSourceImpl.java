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

public interface CommandSourceImpl {

	CommandSource create(String args);

	CommandSource createEmpty();

	class Provider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
															ExtensionContext context) throws Exception {
			CommandSourceImpl impl1 = new CommandSourceImpl() {
				@Override
				public CommandSource create(String args) {
					return new CommandSource.OfArray(args.split(" ", -1));
				}

				@Override
				public CommandSource createEmpty() {
					return new CommandSource.OfArray();
				}
			};
			CommandSourceImpl impl2 = new CommandSourceImpl() {
				@Override
				public CommandSource create(String args) {
					return new CommandSource.OfString(args);
				}

				@Override
				public CommandSource createEmpty() {
					CommandSource source = create("");
					source.next();
					return source;
				}
			};
			class Impl3 implements CommandSourceImpl {

				private final CommandSourceImpl inner;

                Impl3(CommandSourceImpl inner) {
                    this.inner = inner;
                }

				@Override
				public CommandSource create(String args) {
					int indexOfSpace = args.indexOf(' ');
					if (indexOfSpace == -1) {
						return inner.create(args);
					} else {
						return new CommandSource.Prepended(
								args.substring(0, indexOfSpace),
								inner.create(args.substring(indexOfSpace + 1))
						);
					}
				}

				@Override
				public CommandSource createEmpty() {
					return inner.createEmpty();
				}
			}
			return Stream.of(impl1, impl2, new Impl3(impl1), new Impl3(impl2)).map(Arguments::of);
		}
	}

}
