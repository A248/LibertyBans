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

package space.arim.libertybans.core.alts;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;

import java.util.ArrayList;
import java.util.List;

public class AltCheckFormatter {

	private final Configs configs;
	private final InternalFormatter formatter;

	@Inject
	public AltCheckFormatter(Configs configs, InternalFormatter formatter) {
		this.configs = configs;
		this.formatter = formatter;
	}

	private AltsSection.Formatting formatting() {
		return configs.getMessagesConfig().alts().formatting();
	}

	private ComponentLike getKind(DetectionKind detectionKind) {
		switch (detectionKind) {
		case NORMAL:
			return formatting().normal();
		case STRICT:
			return formatting().strict();
		default:
			throw new IllegalArgumentException("Unknown kind " + detectionKind);
		}
	}

	private ComponentText getUsernameFormat(PunishmentType type) {
		var nameDisplay = formatting().nameDisplay();
		if (type == null) {
			return nameDisplay.notPunished();
		}
		switch (type) {
		case BAN:
			return nameDisplay.banned();
		case MUTE:
			return nameDisplay.muted();
		default:
			throw new IllegalArgumentException("Punishment type " + type + " not found");
		}
	}

	private ComponentLike formatUsername(DetectedAlt detectedAlt) {
		return getUsernameFormat(detectedAlt.punishmentType().orElse(null))
				.replaceText("%USERNAME%", detectedAlt.relevantUserName());
	}

	private ComponentLike formatDetectedAlt(DetectedAlt detectedAlt) {
		return formatting().layout()
				.replaceText("%ADDRESS%", detectedAlt.relevantAddress().toString())
				.replaceText("%RELEVANT_USERID%", detectedAlt.relevantUserId().toString())
				.replaceText("%DATE_RECORDED%", formatter.formatAbsoluteDate(detectedAlt.dateAccountRecorded()))
				.asComponent()
				.replaceText((config) -> {
					config.matchLiteral("%DETECTION_KIND%").replacement(getKind(detectedAlt.detectionKind()));
				})
				.replaceText((config) -> {
					config.matchLiteral("%RELEVANT_USER%").replacement(formatUsername(detectedAlt));
				});
	}

	public Component formatMessage(ComponentText header, String target, List<DetectedAlt> detectedAlts) {
		List<ComponentLike> messages = new ArrayList<>(detectedAlts.size() + 1);
		messages.add(formatter.prefix(header.replaceText("%TARGET%", target)));
		for (DetectedAlt detectedAlt : detectedAlts) {
			messages.add(Component.newline());
			messages.add(formatDetectedAlt(detectedAlt));
		}
		return TextComponent.ofChildren(messages.toArray(ComponentLike[]::new));
	}

}
