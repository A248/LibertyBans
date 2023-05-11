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

package space.arim.libertybans.core.commands;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;
import space.arim.libertybans.core.commands.extra.ArgumentParser;
import space.arim.libertybans.core.commands.usage.UsageGlossary;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.event.FireEventWithTimeout;
import space.arim.omnibus.DefaultOmnibus;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

public final class CommandSetupExtension implements ParameterResolver {

	private final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(getClass());

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> type = parameterContext.getParameter().getType();
		return type.equals(AbstractSubCommandGroup.Dependencies.class)
				|| type.equals(Configs.class)
				|| type.equals(ArgumentParser.class)
				|| type.equals(UsageGlossary.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> type = parameterContext.getParameter().getType();

		ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
		Configs configs = store.getOrComputeIfAbsent(
				Configs.class, Mockito::mock, Configs.class);
		ArgumentParser argParser = store.getOrComputeIfAbsent(
				ArgumentParser.class, Mockito::mock, ArgumentParser.class);
		UsageGlossary usage = store.getOrComputeIfAbsent(
				UsageGlossary.class, Mockito::mock, UsageGlossary.class);
		if (type.equals(Configs.class)) {
			return configs;
		}
		if (type.equals(ArgumentParser.class)) {
			return argParser;
		}
		if (type.equals(UsageGlossary.class)) {
			return usage;
		}
		assert type.equals(AbstractSubCommandGroup.Dependencies.class);
		return extensionContext.getStore(NAMESPACE).getOrComputeIfAbsent(type, (k) -> {
			return new AbstractSubCommandGroup.Dependencies(
					new IndifferentFactoryOfTheFuture(),
					new FireEventWithTimeout(new DefaultOmnibus()),
					configs,
					argParser);
		});
	}
}
