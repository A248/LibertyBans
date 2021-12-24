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

package space.arim.libertybans.api.punish;

/**
 * How a punishment can be enforced or "unenforced." <br>
 * <br>
 * These options apply either when a punishment is enacted (where it is enforced)
 * or when a punishment is undone (where it is "unenforced"). <br>
 * <br>
 * Instances of this interface can be obtained wherever it is required as a parameter.
 * For example, for use in {@link Punishment}, use {@link Punishment#enforcementOptionsBuilder()}
 *
 */
public interface EnforcementOptions {

	/**
	 * The enforcement option
	 *
	 * @return enforcement
	 */
	Enforcement enforcement();

	/**
	 * The broadcast option
	 *
	 * @return broadcasting
	 */
	Broadcasting broadcasting();

	/**
	 * How the punishment can be enforced. <br>
	 * <br>
	 * With regards to enacting punishments, enforcement means the kicking of players
	 * and the sending of messages to them. If a punishment is not enforced, no players
	 * are kicked and no messages are sent to them. <br>
	 * <br>
	 * With regards to undoing punishments, unenforcement means the purging of any
	 * relevant caches related to punishments (such as a mute cache).
	 *
	 */
	enum Enforcement {
		/**
		 * The punishment is enforced on the entire network of servers (if applicable) or
		 * single server. As necessary, the punishment is sent to other instances. <br>
		 * <br>
		 * This is the normal mode of punishment enforcement
		 *
		 */
		GLOBAL,
		/**
		 * The punishment is enforced only on the current server. It is not sent to other instances.
		 *
		 */
		SINGLE_SERVER_ONLY,
		/**
		 * No enforcement is performed
		 *
		 */
		NONE;

		/**
		 * Convenience method to check whether this is {@link #GLOBAL}
		 *
		 * @return true if this is the global option
		 */
		public boolean isGlobal() {
			return this == GLOBAL;
		}
	}

	/**
	 * How, and whether, broadcast messages should be sent relating to enaction or undoing of the punishment. <br>
	 * <br>
	 * Please note that the scope of any available broadcasts are limited by the {@link Enforcement}
	 * option. For example, if {@link Enforcement#SINGLE_SERVER_ONLY} is used, broadcasts will only
	 * be sent on the current server. If {@link Enforcement#NONE} is used, no broadcasts will be sent,
	 * and it will be as if {@link #NONE} was specified. <br>
	 * <br>
	 * The default value if {@link #NONE}
	 *
	 */
	enum Broadcasting {
		/**
		 * Broadcasting behavior commonly used with plugin functionality. Sends broadcast messages
		 * to users with the relevant permission.
		 */
		NORMAL,
		/**
		 * Sends broadcast messages as if the punishment has been made "silently", and only to
		 * users with the relevant permissions to see silent broadcast messages.
		 */
		SILENT,
		/**
		 * Sends no broadcast messages. This is the default for API calls
		 */
		NONE
	}

	/**
	 * Builder of {@link EnforcementOptions}
	 *
	 */
	interface Builder {

		/**
		 * Sets the enforcement on this builder
		 *
		 * @param enforcement the enforcement
		 * @return this builder
		 */
		Builder enforcement(Enforcement enforcement);

		/**
		 * Sets the broadcasting option on this builder
		 *
		 * @param broadcasting the broadcasting
		 * @return this builder
		 */
		Builder broadcasting(Broadcasting broadcasting);

		/**
		 * Builds into enforcement options. May be used repeatedly without side effects
		 *
		 * @return the built options
		 */
		EnforcementOptions build();

	}
}
