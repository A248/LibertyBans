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
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.pagination.InstantThenUUID;
import space.arim.libertybans.core.database.pagination.KeysetPage;

public class AltCheckFormatter {

	private final Configs configs;
	private final InternalFormatter formatter;

	@Inject
	public AltCheckFormatter(Configs configs, InternalFormatter formatter) {
		this.configs = configs;
		this.formatter = formatter;
	}

	public Component formatMessage(AccountListFormatting formatting,
								   KeysetPage<DetectedAlt, InstantThenUUID> response,
								   String target, int page) {
		return new FormatAccounts<>(formatting, response).format(
				target, page, new DetectedAltFormat(configs, formatter)
		);
	}

	private record DetectedAltFormat(Configs configs, InternalFormatter formatter)
			implements FormatAccounts.ElementFormat<DetectedAlt> {

		private ComponentLike formatUsername(DetectedAlt detectedAlt) {
			PunishmentType type;
			if (detectedAlt.hasActivePunishment(PunishmentType.BAN)) {
				type = PunishmentType.BAN;
			} else if (detectedAlt.hasActivePunishment(PunishmentType.MUTE)) {
				type = PunishmentType.MUTE;
			} else {
				type = null;
			}
			ComponentText usernameFormat;
			var nameDisplay = configs.getMessagesConfig().alts().formatting().nameDisplay();
			if (type == null) {
				usernameFormat = nameDisplay.notPunished();
			} else {
				usernameFormat = switch (type) {
					case BAN -> nameDisplay.banned();
					case MUTE -> nameDisplay.muted();
					default -> throw new IllegalArgumentException("Punishment type " + type + " not available");
				};
			}
			return usernameFormat.replaceText("%USERNAME%", detectedAlt.latestUsername().orElseGet(
					() -> configs.getMessagesConfig().formatting().victimDisplay().playerNameUnknown()
			));
		}

		@Override
		public ComponentLike format(String target, DetectedAlt detectedAlt) {
			var formatting = configs.getMessagesConfig().alts().formatting();
			return formatting.layout()
					.replaceText("%ADDRESS%", detectedAlt.address().toString())
					.replaceText("%RELEVANT_USERID%", detectedAlt.uuid().toString())
					.replaceText("%DATE_RECORDED%", formatter.formatAbsoluteDate(detectedAlt.recorded()))
					.asComponent()
					.replaceText((config) -> {
						Component detectionKind = switch (detectedAlt.detectionKind()) {
							case NORMAL -> formatting.normal();
							case STRICT -> formatting.strict();
						};
						config.matchLiteral("%DETECTION_KIND%").replacement(detectionKind);
					})
					.replaceText((config) -> {
						config.matchLiteral("%RELEVANT_USER%").replacement(formatUsername(detectedAlt));
					});
		}
	}

}
