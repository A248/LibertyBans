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

package space.arim.libertybans.core.commands.extra;

import org.slf4j.LoggerFactory;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;

@ConfHeader({"Handles how staff are permitted to specify reasons"})
public interface ReasonsConfig {

	enum UnspecifiedReasonBehavior {
		USE_EMPTY_REASON,
		REQUIRE_REASON,
		SUBSTITUTE_DEFAULT
	}

	/**
	 * Gets the unspecified reason behavior taking into account compatibility concerns
	 *
	 * @return the unspecified reason behavior
	 */
	default UnspecifiedReasonBehavior effectiveUnspecifiedReasonBehavior() {
		if (permitBlank()) {
			LoggerFactory.getLogger(ReasonsConfig.class).warn(
					"Detected use of deprecated config option reasons.permit-blank. " +
							"Please switch to unspecified-reason-behavior");
			return UnspecifiedReasonBehavior.USE_EMPTY_REASON;
		}
		return unspecifiedReasonBehavior();
	}

	@ConfKey("permit-blank")
	@ConfComments({"Deprecated option kept for compatibility purposes. Ignore this."})
	@ConfDefault.DefaultBoolean(false)
	/*
	The former handling of this option, which must be retained for compatibility, is as follows:
	If permitBlank is true, use the empty reason "" as the reason (USE_EMPTY_REASON)
	Otherwise, substitute the default reason as the reason (SUBSTITUTE_DEFAULT)
	 */
	boolean permitBlank();

	/**
	 * Do not call this directly, use {@link #effectiveUnspecifiedReasonBehavior()}
	 *
	 * @return the literally input unspecified reason behavior
	 */
	@ConfKey("unspecified-reason-behavior")
	@ConfComments({"When entering commands, what happens if the staff member does not specify a reason?",
			"USE_EMPTY_REASON - Keep the reason blank, as specified.",
			"REQUIRE_REASON - Deny the command; send the usage message.",
			"SUBSTITUTE_DEFAULT - Substitute the default reason."})
	/*
	It is important that the default value, SUBSTITUTE_DEFAULT, matches permitBlank=false
	This way, users on the default config will not see any changes from the introduction
	of this config option.
	 */
	@ConfDefault.DefaultString("SUBSTITUTE_DEFAULT")
	UnspecifiedReasonBehavior unspecifiedReasonBehavior();

	@ConfKey("default-reason")
	@ConfComments("If unspecified-reason-behavior is SUBSTITUTE_DEFAULT, what is the default reason to use when staff do not specify a reason?")
	@ConfDefault.DefaultString("No reason stated.")
	String defaultReason();

}
