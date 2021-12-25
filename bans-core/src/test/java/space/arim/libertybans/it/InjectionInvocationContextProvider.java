/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.it;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import space.arim.injector.Injector;
import space.arim.omnibus.util.ThisClass;

import java.util.List;
import java.util.stream.Stream;

public class InjectionInvocationContextProvider implements TestTemplateInvocationContextProvider {

	private static final Namespace NAMESPACE = Namespace.create(ThisClass.get());

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {

		boolean throwaway = context.getRequiredTestClass().isAnnotationPresent(ThrowawayInstance.class);

		ResourceCreator creator = new ResourceCreator(context.getRoot().getStore(NAMESPACE));
		return new ConfigSpecPossiblities(context.getElement().orElse(null))
				.getAll()
				.flatMap((throwaway) ? creator::createIsolated : creator::create)
				.map((injector) -> new InjectorInvocationContext(injector, throwaway));
	}

	private static class InjectorInvocationContext implements TestTemplateInvocationContext {

		private final Injector injector;
		private final boolean throwaway;

		InjectorInvocationContext(Injector injector, boolean throwaway) {
			this.injector = injector;
			this.throwaway = throwaway;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			var parameterResolver = new InjectorParameterResolver(injector);
			if (throwaway) {
				return List.of(parameterResolver);
			}
			return List.of(parameterResolver, new InjectorCleanupCallback(injector));
		}

	}

}
