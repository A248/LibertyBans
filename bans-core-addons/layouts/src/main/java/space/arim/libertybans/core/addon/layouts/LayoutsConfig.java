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

package space.arim.libertybans.core.addon.layouts;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.addon.AddonConfig;
import space.arim.libertybans.core.commands.extra.DurationParser;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.config.PunishmentAdditionSection;
import space.arim.libertybans.core.config.VictimPermissionSection;
import space.arim.libertybans.core.scope.ConfiguredScope;
import space.arim.libertybans.core.scope.GlobalScope;

import java.util.Map;

public interface LayoutsConfig extends AddonConfig, PunishmentAdditionSection {

	@ConfDefault.DefaultString("&cUsage: /libertybans punish <victim> <track>")
	@Override
	Component usage();

	@SubSection
	@Override
	LayoutsPermissionSection permission();

	@ConfHeader({
			"To use layouts, a staff member needs multiple permissions: ",
			"- libertybans.addon.layout.command",
			"- libertybans.addon.layout.use.<track>.target.uuid -- punish players",
			"- libertybans.addon.layout.use.<track>.target.ip -- punish IP addresses",
			"- libertybans.addon.layout.use.<track>.target.both -- punish player and IP address in the same punishment",
			"",
			"For simplicity, you may use wildcard permissions, e.g. 'libertybans.addon.layout.use.hacking.target.*'",
			"will allow punishing using the 'hacking' track no matter whether a player or IP address is punished"
	})
	interface LayoutsPermissionSection extends VictimPermissionSection {

		@ConfKey("layouts-generally")
		@ConfComments("The message when the general permission is missing")
		@ConfDefault.DefaultString("&cSorry, you cannot use layouts.")
		Component layoutsGenerally();

	}

	@ConfKey("track-does-not-exist")
	@ConfDefault.DefaultString("&cThe track '%TRACK_ARG%' does not exist.")
	ComponentText trackDoesNotExist();

	@ConfDefault.DefaultString("&c&o%TARGET%&r&7 cannot be punished.")
	@Override
	ComponentText exempted();

	@ConfComments({
			"Depending on how you configure the punishment tracks, sometimes the calculated punishment may conflict",
			"with an existing punishment on the same user. For example, maybe the target user is already banned, but",
			"the calculated punishment is a ban. In these cases a conflict will arise and the following message sent.",
			"Usually, it is ideal to structure your punishment tracks so that staff members do not face this problem"
	})
	@ConfDefault.DefaultString("&7The punishment cannot be added due to a conflict (i.e. &e%TARGET%&7 is already banned or muted).")
	@Override
	ComponentText conflicting();

	@ConfKey("success-message")
	@ConfComments({
			"The success message sent to the staff member. Note that the punishment notification uses the main configuration."
	})
	@ConfDefault.DefaultString("&aPunished &c&o%VICTIM%&r&a (&e%TYPE%&a) for &a&o%DURATION%&r&a because of &e&o%REASON%&r&a.")
	@Override
	ComponentText successMessage();

	@Override
	default ComponentText successNotification() {
		throw new UnsupportedOperationException();
	}

	@ConfComments({
			"The tracks according to which punishment details are calculated",
			"",
			"The label for each section determines the track ID to be stored in the database. It must be lowercase.",
			"Changing the track ID will create a new track and old punishments will not be updated.",
			"The permission libertybans.addon.layout.use.<id> is required for staff members to use the track.",
			"",
			"count-active:",
			"Whether to count active punishments on the player, or all punishments including those expired or undone.",
			"If disabled, the punishment count will include revoked and/or expired punishments.",
			"",
			"progressions:",
			"The punishment details at each level of progression. The number represents the amount of existing",
			"relevant punishments on the victim before the progression will be triggered. Only active punishments",
			"of the same track are used to compute the amount. If a progression is not specified for an amount,",
			"the greatest previous number is used. There must be a progression specified for the first punishment."
	})
	@ConfDefault.DefaultObject("defaultTracks")
	Map<String, Track.@SubSection Ladder> tracks();

	@ConfKey("clear-track-when-punishment-revoked")
	@ConfComments({
			"If enabled, undoing punishments (/unban, /unmute, /unwarn) will have the additional effect of erasing",
			"a punishment's track. This will mean that the revoked punishment will no longer be counted towards",
			"the number of punishments on that track, which is useful for tracks with 'count-active: false' but you",
			"still want to exclude revoked punishments."
	})
	@ConfDefault.DefaultBoolean(false)
	boolean clearTrackWhenPunishmentRevoked();

	static Map<String, Track.Ladder> defaultTracks() {

		record SimpleLadder(boolean countActive,
							Map<Integer, Track.Ladder.Progression> progressions) implements Track.Ladder {}

		record SimpleProgression(PunishmentType type, String reason, ParsedDuration duration, ConfiguredScope scope)
				implements Track.Ladder.Progression {

			SimpleProgression(PunishmentType type, String reason, String duration) {
				this(
						type, reason,
						new ParsedDuration(duration, new DurationParser().parse(duration)),
						ConfiguredScope.defaultPunishingScope()
				);
			}
		}
		return Map.of(
				"hacking",
				new SimpleLadder(
						false,
						Map.of(
								1, new SimpleProgression(PunishmentType.BAN, "No hacking allowed", "40d"),
								2, new SimpleProgression(PunishmentType.BAN, "You are a hacker and will never be unbanned", "perm")
						)
				),
				"spamming",
				new SimpleLadder(
						true,
						Map.of(
								1, new SimpleProgression(PunishmentType.WARN, "Don't spam", "15d"),
								2, new SimpleProgression(PunishmentType.WARN, "If you spam again, you will be muted", "perm"),
								3, new SimpleProgression(PunishmentType.MUTE, "That's enough spamming", "30d"),
								4, new SimpleProgression(PunishmentType.MUTE, "You spammed too much; never again", "perm")
						)
				)
		);
	}

}
