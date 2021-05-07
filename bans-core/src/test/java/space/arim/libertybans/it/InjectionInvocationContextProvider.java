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
import java.util.Set;
import java.util.stream.Stream;

public class InjectionInvocationContextProvider implements TestTemplateInvocationContextProvider {

	private static final Namespace NAMESPACE = Namespace.create(ThisClass.get());

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Set<ConfigSpec> possibilities = new ConfigSpecPossiblities().getAllFiltered(context.getElement().orElse(null));
		ResourceCreator creator = new ResourceCreator(context.getRoot().getStore(NAMESPACE));
		return possibilities.stream().map((spec) -> creator.create(spec)).map(InjectorInvocationContext::new);
	}

	private static class InjectorInvocationContext implements TestTemplateInvocationContext {

		private final Injector injector;

		InjectorInvocationContext(Injector injector) {
			this.injector = injector;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return List.of(new LibertyBansIntegrationExtension(injector));
		}

	}

}
