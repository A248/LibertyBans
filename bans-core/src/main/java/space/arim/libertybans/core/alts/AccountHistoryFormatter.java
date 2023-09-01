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

package space.arim.libertybans.core.alts;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.user.KnownAccount;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;

import java.time.Instant;
import java.util.List;

public class AccountHistoryFormatter {

	private final Configs configs;
	private final ListFormat<KnownAccount> listFormat;

	@Inject
	public AccountHistoryFormatter(Configs configs, InternalFormatter formatter) {
		this.configs = configs;
		listFormat = new ListFormat<>(formatter, new KnownAccountFormat(configs, formatter));
	}

	public Component formatMessage(String target, List<? extends KnownAccount> knownAccounts) {
		ComponentText header = configs.getMessagesConfig().accountHistory().listing().header();
		return listFormat.formatMessage(header, target, knownAccounts);
	}

	private record KnownAccountFormat(Configs configs, InternalFormatter formatter)
			implements ListFormat.ElementFormat<KnownAccount> {

		@Override
		public ComponentLike format(String target, KnownAccount knownAccount) {
			Instant recorded = knownAccount.recorded();
			return configs.getMessagesConfig().accountHistory().listing().layout()
					.replaceText("%TARGET%", target)
					.replaceText("%USERNAME%", knownAccount.latestUsername().orElseGet(
							() -> configs.getMessagesConfig().formatting().victimDisplay().playerNameUnknown()
					))
					.replaceText("%ADDRESS%", knownAccount.address().toString())
					.replaceText("%DATE_RECORDED%", formatter.formatAbsoluteDate(recorded))
					.replaceText("%DATE_RECORDED_RAW%", Long.toString(recorded.getEpochSecond()));
		}
	}
}
