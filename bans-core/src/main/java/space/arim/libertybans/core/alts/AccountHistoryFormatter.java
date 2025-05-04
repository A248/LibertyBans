/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
import space.arim.libertybans.api.user.KnownAccount;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.pagination.KeysetPage;

import java.time.Instant;

public class AccountHistoryFormatter {

	private final Configs configs;
	private final InternalFormatter formatter;

	@Inject
	public AccountHistoryFormatter(Configs configs, InternalFormatter formatter) {
		this.configs = configs;
		this.formatter = formatter;
	}

	public Component formatMessage(KeysetPage<KnownAccount, Instant> response, String target, int page) {
		return new FormatAccounts<>(configs.getMessagesConfig().accountHistory().listing(), response).format(
				target, page, new KnownAccountFormat(configs, formatter)
		);
	}

	private record KnownAccountFormat(Configs configs, InternalFormatter formatter)
			implements FormatAccounts.ElementFormat<KnownAccount> {

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
