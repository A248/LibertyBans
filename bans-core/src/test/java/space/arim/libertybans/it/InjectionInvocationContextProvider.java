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

package space.arim.libertybans.it;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import space.arim.injector.Injector;
import space.arim.omnibus.util.ThisClass;

import java.util.ArrayList;
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
		boolean irrelevantData = context.getRequiredTestMethod().isAnnotationPresent(IrrelevantData.class);

		ResourceCreator creator = new ResourceCreator(context.getRoot().getStore(NAMESPACE));
		return new ConfigSpecPossiblities(context.getElement().orElseThrow())
				.getAll()
				.flatMap((throwaway) ? creator::createIsolated : creator::create)
				.map((injector) -> new InjectorInvocationContext(injector, throwaway, irrelevantData));
	}

	private record InjectorInvocationContext(Injector injector, boolean throwaway,
											 boolean irrelevantData) implements TestTemplateInvocationContext {

		@Override
			public List<Extension> getAdditionalExtensions() {
				List<Extension> extensions = new ArrayList<>(3);
				extensions.add(new InjectorParameterResolver(injector));
				if (!throwaway) {
					extensions.add(new InjectorCleanupCallback(injector));
				}
				if (irrelevantData) {
					extensions.add(new IrrelevantDataCallback(injector));
				}
				return extensions;
			}

		}

}
