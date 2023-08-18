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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import space.arim.injector.Identifier;
import space.arim.injector.Injector;
import space.arim.injector.InjectorBuilder;
import space.arim.injector.SpecificationSupport;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.CommandsModule;
import space.arim.libertybans.core.PillarOneReplacementModule;
import space.arim.libertybans.core.PillarTwoBindModule;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.it.env.QuackBindModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

class ResourceCreator {

	private final Store store;

	ResourceCreator(Store store) {
		this.store = store;
	}

	Stream<Injector> create(ConfigSpec configSpec) {
		return createUsing(configSpec, ConfigSpecWithDatabase::new);
	}

	Stream<Injector> createIsolated(ConfigSpec configSpec) {
		return createUsing(configSpec, (db, spec) -> new InstanceKey() {});
	}

	private Stream<Injector> createUsing(ConfigSpec configSpec,
										 BiFunction<ConfigSpec, DatabaseInstance, InstanceKey> keyFunction) {
		return DatabaseInstance
				.matchingVendor(configSpec.vendor())
				.map((database) -> {
					InstanceKey instanceKey = keyFunction.apply(configSpec, database);
					return createSingle(configSpec, database, instanceKey);
				})
				.flatMap(Optional::stream);
	}

	private Optional<Injector> createSingle(ConfigSpec configSpec, DatabaseInstance database, InstanceKey instanceKey) {

		DatabaseInfo databaseInfo = database.createInfo().orElse(null);
		if (databaseInfo == null) {
			return Optional.empty();
		}

		return Optional.of(store.getOrComputeIfAbsent(instanceKey, (ignore) -> {

			Path tempDirectory = createTempDirectory();

			Injector injector = new InjectorBuilder()
					.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), tempDirectory)
					.bindInstance(InstanceType.class, InstanceType.PROXY)
					.bindInstance(ConfigSpec.class, configSpec)
					.bindInstance(DatabaseInfo.class, databaseInfo)
					.addBindModules(
							new ApiBindModule(),
							new PillarOneReplacementModule(),
							new PillarTwoBindModule(),
							new CommandsModule(),
							new QuackBindModule())
					.specification(SpecificationSupport.JAKARTA)
					.multiBindings(true)
					.build();

			BaseFoundation base = injector.request(BaseFoundation.class);
			base.startup();
			return new BaseWrapper(injector, base, tempDirectory);

		}, BaseWrapper.class).injector);
	}

	private static Path createTempDirectory() {
		Path tempDir;
		try {
			tempDir = Files.createTempDirectory("libertybans-test-dir");
		} catch (IOException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
		return tempDir;
	}
}
