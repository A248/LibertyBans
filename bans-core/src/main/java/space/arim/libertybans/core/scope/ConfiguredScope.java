/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.scope;

import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;

import java.util.Objects;
import java.util.Optional;

public final class ConfiguredScope {

	private final @Nullable ServerScope scope;

	private ConfiguredScope(@Nullable ServerScope scope) {
		this.scope = scope;
	}

	Optional<ServerScope> rawOptional() {
		return Optional.ofNullable(scope);
	}

	public static ConfiguredScope create(ServerScope scope) {
		Objects.requireNonNull(scope);
		return new ConfiguredScope(scope);
	}

	public static ConfiguredScope defaultPunishingScope() {
		return new ConfiguredScope(null);
	}

	public ServerScope actualize(ScopeManager scopeManager) {
		return scope == null ? scopeManager.defaultPunishingScope() : scope;
	}

	public static final class Serializer implements ValueSerialiser<ConfiguredScope> {

		private final ScopeParsing scopeParsing = new ScopeParsing();

		@Override
		public Class<ConfiguredScope> getTargetClass() {
			return ConfiguredScope.class;
		}

		@Override
		public ConfiguredScope deserialise(FlexibleType flexibleType) throws BadValueException {
			String input = flexibleType.getString();
			if (input.isEmpty()) {
				return ConfiguredScope.defaultPunishingScope();
			}
			ServerScope scope;
			try {
				scope = scopeParsing.parseFrom(input);
			} catch (IllegalArgumentException ex) {
				throw flexibleType.badValueExceptionBuilder()
						.message("Invalid scope details")
						.cause(ex)
						.build();
			}
			return new ConfiguredScope(scope);
		}

		@Override
		public Object serialise(ConfiguredScope value, Decomposer decomposer) {
			ServerScope scope = value.scope;
			if (scope == null) {
				return "";
			}
			return scopeParsing.display(scope, ScopeParsing.GLOBAL_SCOPE_USER_INPUT);
		}
	}
}
