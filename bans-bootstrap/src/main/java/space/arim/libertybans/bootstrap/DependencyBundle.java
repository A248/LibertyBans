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

package space.arim.libertybans.bootstrap;

import space.arim.libertybans.bootstrap.depend.Dependency;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.depend.ExistingDependency;
import space.arim.libertybans.bootstrap.depend.ExtractNestedJars;
import space.arim.libertybans.bootstrap.depend.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

enum DependencyBundle {

	CAFFEINE(Repositories.CENTRAL_REPO),
	JAKARTA(Repositories.CENTRAL_REPO),
	KYORI(Repositories.CENTRAL_REPO),
	SELF_IMPLEMENTATION(Repositories.ARIM_AFFERO_GPL3),
	SLF4J(Repositories.CENTRAL_REPO);

	private final Repository repository;

	DependencyBundle(Repository repository) {
		this.repository = repository;
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT).replace("_", "-");
	}

	ExistingDependency existingDependency() {
		String jarResourceName = this + "-bundle.jar";
		URL jarResource = getClass().getResource("/dependencies/jars/" + jarResourceName);
		if (jarResource == null) {
			throw new IllegalStateException("Cannot find nested jar resource for " + this);
		}
		return new ExtractNestedJars(jarResource, jarResourceName);
	}

	void prepareToDownload(DependencyLoaderBuilder loader) {
		List<Dependency> dependencies;
		try {
			dependencies = readDependencies();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		for (Dependency dependency : dependencies) {
			loader.addDependencyPair(dependency, repository);
		}
	}

	private InputStream readResource() throws IOException {
		String resourcePath = "/dependencies/" + this;
		URL resourceUrl = getClass().getResource(resourcePath);
		Objects.requireNonNull(resourceUrl, "internal error, missing " + resourcePath);
		return resourceUrl.openStream();
	}

	private IllegalArgumentException malformatted(String reason) {
		return new IllegalArgumentException(
				"Dependency file for " + this + " is malformatted. Reason: " + reason);
	}

	private List<Dependency> readDependencies() throws IOException {
		List<Dependency> dependencies = new ArrayList<>();
		BlockingQueue<String> readDetails = new ArrayBlockingQueue<>(4);
		String nextLine;
		try (InputStream inputStream = readResource();
			 InputStreamReader unbufferedReader = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
			 BufferedReader reader = new BufferedReader(unbufferedReader)) {

			while ((nextLine = reader.readLine()) != null) {
				if (nextLine.isEmpty()) {
					dependencies.add(dependencyFrom(readDetails));
					continue;
				}
				if (!readDetails.offer(nextLine)) {
					throw malformatted("Too many details");
				}
			}
		}
		if (!readDetails.isEmpty()) {
			dependencies.add(dependencyFrom(readDetails));
		}
		return dependencies;
	}

	private Dependency dependencyFrom(BlockingQueue<String> readDetails) {
		if (readDetails.remainingCapacity() != 0) {
			throw malformatted("Lacking details, received only " + readDetails);
		}
		return Dependency.of(
				readDetails.remove(), readDetails.remove(), readDetails.remove(), readDetails.remove()
		);
	}

}
