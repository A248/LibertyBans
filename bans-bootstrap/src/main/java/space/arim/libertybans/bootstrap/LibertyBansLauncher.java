/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import java.io.File;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.DefaultDependencyLoader;
import space.arim.libertybans.bootstrap.depend.Dependency;
import space.arim.libertybans.bootstrap.depend.DependencyLoader;
import space.arim.libertybans.bootstrap.depend.Repository;

public class LibertyBansLauncher {

	private final BootstrapLauncher launcher;
	private final Function<Class<?>, String> getPluginFor;
	
	public LibertyBansLauncher(File folder, Executor executor, Function<Class<?>, String> getPluginFor) {
		ClassLoader ownClassLoader = getClass().getClassLoader();
		if (!(ownClassLoader instanceof URLClassLoader)) {
			throw new IllegalStateException("LibertyBans must be loaded through a URLClassLoader");
		}
		this.getPluginFor = getPluginFor;

		File depsFolder = new File(folder, "libs");
		DependencyLoader apiDepLoader = new DefaultDependencyLoader();
		DependencyLoader internalDepLoader = new DefaultDependencyLoader();
		apiDepLoader.setExecutor(executor).setOutputDirectory(new File(depsFolder, "api-deps"));
		internalDepLoader.setExecutor(executor).setOutputDirectory(new File(depsFolder, "internal-deps"));
		
		addApiDeps(apiDepLoader);
		addInternalDeps(internalDepLoader);

		launcher = new BootstrapLauncher("LibertyBans", (URLClassLoader) ownClassLoader, apiDepLoader, internalDepLoader);
	}
	
	private static Class<?> forName(String clazzname) {
		try {
			Class<?> result = Class.forName(clazzname);
			return result;
		} catch (ClassNotFoundException ignored) {
			return null;
		}
	}
	
	private static boolean classExists(String clazzname) {
		return forName(clazzname) != null;
	}
	
	private static final Repository ARIM_REPO = new Repository("https://www.arim.space/maven");
	private static final Repository CENTRAL_REPO = new Repository("https://repo.maven.apache.org/maven2");
	
	private void addApiDeps(DependencyLoader loader) {
		if (!classExists("space.arim.universal.registry.UniversalRegistry")) {
			loader.addPair(Dependency.of("space.arim.universal", "universal-all-shaded", "0.13.4-SNAPSHOT",
					"7542196eb4ad373758f8d75659f0986556b9656190ea69210bfe87b631c6d61b16a1cd871d79628f242920819862b93e081db9493a11164a0a0d53ab8e8e41a4"),
					ARIM_REPO);
		}
		if (!classExists("space.arim.uuidvault.api.UUIDVault")) {
			loader.addPair(Dependency.of("space.arim.uuidvault", "assemble", "0.4.5-SNAPSHOT",
					"843d3c3ade4aee537418f4f8abe6f186a3bb0197c3bf857b6585eecd8d78d611d325058217c1ee5bfd617f6cb9a734bdd6f241ae5fb23c31a7559e00a8638e52"),
					ARIM_REPO);
		}
	}
	
	private void addInternalDeps(DependencyLoader loader) {
		/*
		 * Since Paper, Waterfall, Sponge, and Velocity include slf4j,
		 * it may already be present
		 */
		if (!classExists("org.slf4j.Logger")) {
			loader.addPair(Dependency.of("org.slf4j", "slf4j-api", "1.7.25",
					"5dd6271fd5b34579d8e66271bab75c89baca8b2ebeaa9966de391284bd08f2d720083c6e0e1edda106ecf8a04e9a32116de6873f0f88c19c049c0fe27e5d820b"),
					CENTRAL_REPO);
			loader.addPair(Dependency.of("org.slf4j", "slf4j-jdk14", "1.7.25",
					"cf73d92dbc6d1963dfc9d842f7c4043b897d21cbafaa2b2f84d52aa4277845cb230aa4987919d86aea888f8005c2263759cbeb14c80c6fe83b22e9a47b7565f4"),
					CENTRAL_REPO);
		}
		Class<?> hikariClass = forName("com.zaxxer.hikari.HikariConfig");
		if (hikariClass == null) {
			loader.addPair(Dependency.of("com.zaxxer", "HikariCP", "3.4.5",
					"4e70e08544e199eed09f5f3681ab5a39681d8d6eb6c9ca2cb6a78535aedbcc66b4292330bbcb40bfb8bf8a6f284d013e55a039542ffc85222d6ac26f816eafd3"),
					CENTRAL_REPO);
		} else {
			warnRelocation("HikariCP", hikariClass);
		}
		Class<?> caffeineClass = forName("com.github.benmanes.caffeine.cache.Caffeine");
		if (caffeineClass == null) {
			loader.addPair(Dependency.of("com.github.ben-manes.caffeine", "caffeine", "2.8.5",
					"9351c0e57aefd58f468ad5eced9c828254e7d19692654eac81cfeba0d72d4d91f5021eaaad533d6c292553f0487a31f92d73b0ee711296ab76ba6d168056a5c6"),
					CENTRAL_REPO);
		} else {
			warnRelocation("Caffeine", caffeineClass);
		}
		/*
		 * We set the driver class name and use the context class loader w/ HikariCP to
		 * ensure only our own HSQLDB is used, even if there is another on the classpath.
		 */
		loader.addPair(Dependency.of("org.hsqldb", "hsqldb", "3.4.5",
					"5539ef60987d6bd801c4ced80ede3138578307122be92dedbf2c6a48ea58db6e5e6c064d36de6f1fc0ccb6991213eb801ec036957edde48a358657e0cb8d4e62"),
					CENTRAL_REPO);
		/*
		 * Since ArimAPI is also a plugin, it may already be present
		 */
		if (!classExists("space.arim.api.util.sql.HikariPoolSqlBackend")) {
			loader.addPair(Dependency.of("space.arim.api", "arimapi-all", "0.16.0-SNAPSHOT",
					"3b401adb0bf63aa8f8ec2884c7cc093fe78cd525425af2a60588f2459b756d9e037bd26280df8271a1b43dc4b16a4c7ca269adcfcdd1c0eef013860931e355dd"),
					ARIM_REPO);
		}
		loader.addPair(Dependency.of("space.arim.libertybans", "bans-dl", "0.1.0-SNAPSHOT",
				"a1e9e4420eb776e328febd29c4ab9accd4405b03432d7d09a79c8a949fd20d824f6c0b95c3ae3c71b205eb6278ccda369777a5133f7dd01b1100b26faa9b6eec"),
				ARIM_REPO);
	}
	
	private void warnRelocation(String libName, Class<?> libClass) {
		String plugin = getPluginFor.apply(libClass);
		warn("Plugin '" + ((plugin == null) ? "Unknown" : plugin) + "' has shaded the library '" + libName
				+ "' but failed to relocate it! "
				+ "This may or may not pose any problems. Contact the author of this plugin and tell them to relocate their dependencies.");
	}
	
	// Must use System.err because we do not know whether the platform uses slf4j or JUL
	private void warn(String message) {
		System.err.println("[LibertyBans] " + message);
	}

	public CompletableFuture<ClassLoader> attemptLaunch() {
		return launcher.loadAll().thenApply((downloaded) -> {
			if (!downloaded) {
				return null;
			}
			return launcher.getInternalClassLoader();
		});
	}
	
}
