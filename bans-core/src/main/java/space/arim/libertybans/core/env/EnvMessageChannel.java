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

package space.arim.libertybans.core.env;

import space.arim.libertybans.core.env.message.PluginMessage;

import java.util.function.Consumer;

public interface EnvMessageChannel<H> {

	/**
	 * Installs a platform specific handler
	 *
	 * @param handler the handler
	 */
	void installHandler(H handler);

	/**
	 * Uninstalls a platform specific handler
	 *
	 * @param handler the handler
	 */
	void uninstallHandler(H handler);

	/**
	 * Wraps an acceptor as a platform specific handler. Should be called once.
	 *
	 * @param acceptor the acceptor
	 * @param pluginMessage the plugin message it handles
	 * @return the handler
	 * @param <R> the type of the handler
	 */
	<R> H createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage);

	static <H> void parameterize(EnvMessageChannel<H> messageChannel, Consumer<EnvMessageChannel<H>> action) {
		action.accept(messageChannel);
	}

	final class NoOp implements EnvMessageChannel<Void> {

		@Override
		public void installHandler(Void handler) {

		}

		@Override
		public void uninstallHandler(Void handler) {

		}

		@Override
		public <R> Void createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
			return null;
		}
	}

}
