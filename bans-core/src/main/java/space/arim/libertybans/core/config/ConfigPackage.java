/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.configure.ConfigResult;
import space.arim.api.configure.Configuration;
import space.arim.api.configure.configs.SingularConfig;
import space.arim.api.configure.yaml.YamlSyntaxException;

public class ConfigPackage {

	final SingularConfig sql;
	final SingularConfig config;
	final Configuration messages;
	
	/**
	 * The executor or executor service for config IO <br>
	 * <br>
	 * LibertyBans tries to use a platform thread pool for this. However,
	 * not all platforms make this easy. In the worst case, an own thread pool
	 * is created and this field is an instance of ExecutorService. <br>
	 * <br>
	 * At shutdown, check if the field is ExecutorService and if so shut it down.
	 * 
	 */
	final Executor readWriteService;
	
	private final Path langFolder;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	ConfigPackage(SingularConfig sql, SingularConfig config, Configuration messages,
			Executor readWriteService, Path langFolder) {
		this.sql = sql;
		this.config = config;
		this.messages = messages;
		this.readWriteService = readWriteService;
		this.langFolder = langFolder;
	}
	
	public CompletableFuture<Boolean> reloadConfigs() {
		Set<CompletableFuture<?>> futureLangFiles = new HashSet<>();
		for (Translation translation : Translation.values()) {
			final String name = "messages_" + translation.name().toLowerCase(Locale.ENGLISH) + ".yml";
			Path messagesPath = langFolder.resolve(name);
			if (!Files.exists(messagesPath)) {
				futureLangFiles.add(CompletableFuture.runAsync(() -> {

					try (InputStream inputStream = getClass().getResource("/lang/" + name).openStream();
							ReadableByteChannel source = Channels.newChannel(inputStream);
							FileChannel dest = FileChannel.open(messagesPath, StandardOpenOption.WRITE,
									StandardOpenOption.CREATE_NEW)) {

						dest.transferFrom(source, 0, Long.MAX_VALUE);
					} catch (IOException ex) {
						logger.warn("Unable to copy language file for language {}", name, ex);
					}
				}, readWriteService));
			}
		}
		var langFilesFuture = CompletableFuture.allOf(futureLangFiles.toArray(CompletableFuture[]::new));
		var readSqlFuture = throwIfFailed(sql.saveDefaultConfig()).thenCompose((copyResult) -> throwIfFailed(sql.readConfig()));
		var readConfFuture = throwIfFailed(config.saveDefaultConfig()).thenCompose((copyResult) -> throwIfFailed(config.readConfig()));
		var readMsgsFuture = readConfFuture.thenCompose((readConfResult) -> {
			String langFileOption = readConfResult.getReadData().getString("lang-file");
			if (langFileOption == null) {
				logger.warn("Unspecified language file, using default (en)");
				langFileOption = "en";
			}
			Path messagesPath = langFolder.resolve("messages_" + langFileOption + ".yml");
			return langFilesFuture.thenCompose((ignore) -> throwIfFailed(messages.readConfig(messagesPath)));
		});
		return CompletableFuture.allOf(readSqlFuture, readConfFuture, readMsgsFuture).handle((ignore, ex) -> {
			if (ex != null) {
				Throwable cause = ex.getCause();
				if (cause instanceof YamlSyntaxException) {
					logger.warn("One or more of your YML files has invalid syntax. "
							+ "Please use a YAML validator such as https://yaml-online-parser.appspot.com/ "
							+ "and paste your config files there to check them.", cause);
				} else {
					logger.warn("An unexpected issue occurred while reloading the configuration.", ex);
				}
				return false;
			}
			return true;
		});
	}
	
	private static <T extends ConfigResult> CompletableFuture<T> throwIfFailed(CompletableFuture<T> futureResult) {
		return futureResult.thenApply((result) -> {
			if (!result.getResultDefinition().isSuccess()) {
				Exception ex = result.getException();
				if (ex instanceof RuntimeException) {
					throw (RuntimeException) ex;
				}
				throw new CompletionException(ex);
			}
			return result;
		});
	}
	
}
