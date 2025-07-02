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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.Formatter;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.database.pagination.*;
import space.arim.libertybans.it.util.RandomUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AltCheckFormatterTest {

	private final AltsSection.Formatting conf;
	private final Formatter formatter;

	private AltCheckFormatter altCheckFormatter;

	public AltCheckFormatterTest(@Mock AltsSection.Formatting conf, @Mock Formatter formatter) {
		this.conf = conf;
		this.formatter = formatter;
	}

	@BeforeEach
	public void setAltCheckFormatter(@Mock Configs configs, @Mock MessagesConfig messagesConfig,
									 @Mock AltsSection altsSection) {
		when(configs.getMessagesConfig()).thenReturn(messagesConfig);
		when(messagesConfig.alts()).thenReturn(altsSection);
		when(altsSection.formatting()).thenReturn(conf);
		altCheckFormatter = new AltCheckFormatter(configs, formatter);
	}

	private AccountListFormatting formattingConfig(Component header, Component footer) {
		return new AccountListFormatting() {
			@Override
			public ComponentText header() {
				return ComponentText.create(header);
			}

			@Override
			public Component separator() {
				return Component.newline();
			}

			@Override
			public ComponentText footer() {
				return ComponentText.create(footer);
			}
		};
	}

	@Test
	public void formatMessage() throws UnknownHostException {
		Component header = Component.text("Alt report for %TARGET%");
		String address = "207.144.101.102";
		UUID userId = UUID.randomUUID();
		String username = "AltUser";
		Instant date = Instant.parse("2021-07-23T02:15:23.000000Z");
		DetectedAlt alt = new DetectedAlt(
				userId, username, NetworkAddress.of(InetAddress.getByName(address)), date, DetectionKind.NORMAL
		);
		{
			when(conf.layout()).thenReturn(ComponentText.create(Component.text(
					"detection_kind: %DETECTION_KIND%, address: %ADDRESS%, username: %RELEVANT_USER%, " +
							"user_id: %RELEVANT_USERID%, date_recorded: %DATE_RECORDED%")));
			when(conf.normal()).thenReturn(Component.text("NORMAL"));
			var nameDisplay = mock(AltsSection.Formatting.NameDisplay.class);
			when(nameDisplay.notPunished()).thenReturn(ComponentText.create(Component.text("%USERNAME%")));
			when(conf.nameDisplay()).thenReturn(nameDisplay);
			when(formatter.prefix(any())).thenAnswer((invocation) -> invocation.getArgument(0));
			when(formatter.formatAbsoluteDate(date)).thenReturn(date.toString());
		}
		assertEquals(
				"Alt report for MainUser\n" +
						"detection_kind: " + alt.detectionKind() + ", address: " + address + ", username: " + username +
						", " + "user_id: " + userId + ", date_recorded: " + date,
				PlainComponentSerializer.plain().serialize(altCheckFormatter.formatMessage(
						formattingConfig(header, Component.empty()),
						new KeysetPage<>(
								List.of(alt), KeysetAnchor.unset(), KeysetAnchor.unset(),
								InstantThenUUID.borderValueHandle()
						),
						"MainUser", -1)
				)
		);
	}

	@Test
	public void formatPunishedAltsWithDetectionKind() {
		Instant date = Instant.parse("2021-07-23T02:15:23.000000Z");
		{
			when(conf.layout()).thenReturn(ComponentText.create(Component.text(
					"kind: %DETECTION_KIND%, username: %RELEVANT_USER%")));
			when(conf.normal()).thenReturn(Component.text("NORMAL"));
			when(conf.strict()).thenReturn(Component.text("STRICT"));
			when(formatter.prefix(any())).thenAnswer((invocation) -> invocation.getArgument(0));
			when(formatter.formatAbsoluteDate(any())).thenReturn("date");

			var nameDisplay = mock(AltsSection.Formatting.NameDisplay.class);
			when(nameDisplay.banned()).thenReturn(ComponentText.create(Component.text("Banned(%USERNAME%)")));
			when(nameDisplay.muted()).thenReturn(ComponentText.create(Component.text("Muted(%USERNAME%)")));
			when(nameDisplay.notPunished()).thenReturn(ComponentText.create(Component.text("None(%USERNAME%)")));
			when(conf.nameDisplay()).thenReturn(nameDisplay);
		}
		List<DetectedAlt> alts = List.of(
				new DetectedAlt(
						UUID.randomUUID(), "BadUser", RandomUtil.randomAddress(), date,
						DetectionKind.NORMAL, PunishmentType.BAN
				),
				new DetectedAlt(
						UUID.randomUUID(), "Misbehaver", RandomUtil.randomAddress(), date,
						DetectionKind.STRICT, PunishmentType.MUTE
				),
				new DetectedAlt(
						UUID.randomUUID(), "Saint", RandomUtil.randomAddress(), date,
						DetectionKind.NORMAL
				)
		);
		InstantThenUUID lastPageAnchor = new InstantThenUUID(alts.get(0).recorded(), alts.get(0).uuid());
		InstantThenUUID nextPageAnchor = new InstantThenUUID(alts.get(2).recorded(), alts.get(2).uuid());
		Component formattedMessage = altCheckFormatter.formatMessage(
				formattingConfig(Component.text("Header"), Component.empty()),
				new KeysetPage<>(
						alts,
						new KeysetAnchor<>(0, lastPageAnchor, false),
						new KeysetAnchor<>( 2, nextPageAnchor, true),
						InstantThenUUID.borderValueHandle()
				),
				"MainUser", 1
		);
		assertEquals("""
						Header
						kind: NORMAL, username: Banned(BadUser)
						kind: STRICT, username: Muted(Misbehaver)
						kind: NORMAL, username: None(Saint)""",
				PlainComponentSerializer.plain().serialize(formattedMessage));
	}
}
