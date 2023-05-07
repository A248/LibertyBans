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

package space.arim.libertybans.core.shortcutreasons;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.addon.shortcutreasons.ShortcutReasonsAddon;
import space.arim.libertybans.core.addon.shortcutreasons.ShortcutReasonsConfig;
import space.arim.libertybans.core.addon.shortcutreasons.ShortcutReasonsListener;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.event.PunishEventImpl;
import space.arim.omnibus.DefaultOmnibus;
import space.arim.omnibus.Omnibus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShortcutReasonsListenerTest {

	private final Omnibus omnibus = new DefaultOmnibus();
	private final ShortcutReasonsConfig config;

	private ShortcutReasonsAddon addon;
	private ShortcutReasonsListener listener;

	public ShortcutReasonsListenerTest(@Mock ShortcutReasonsConfig config) {
		this.config = config;
	}

	@BeforeEach
	public void setupListener(@Mock AddonCenter addonCenter) {
		addon = new ShortcutReasonsAddon(addonCenter, omnibus, () -> listener);
		listener = new ShortcutReasonsListener(addon);
		when(addonCenter.configurationFor(addon)).thenReturn(config);

		addon.startup();
	}

	private PunishEventImpl fireEvent(DraftPunishment originalPunishment) {
		PunishEventImpl event = new PunishEventImpl(originalPunishment, mock(CmdSender.class));
		return omnibus.getEventBus().fireAsyncEvent(event).join();
	}

	@Test
	public void noMatch(@Mock DraftPunishment draftPunishment) {
		when(config.shortcutIdentifier()).thenReturn("#");
		when(draftPunishment.getReason()).thenReturn("no match");
		var event = fireEvent(draftPunishment);
		assertSame(draftPunishment, event.getDraftSanction());
		verifyNoMoreInteractions(event.getSender());
	}

	@Test
	public void simpleSubstitute(@Mock DraftPunishment draftPunishment,
								 @Mock DraftPunishment newPunishment, @Mock DraftPunishmentBuilder newBuilder) {
		when(config.shortcutIdentifier()).thenReturn("#");
		when(config.shortcuts()).thenReturn(Map.of(
				"hacking", "hello hackers",
				"spamming", "don't be a spammer, thank you"
		));

		when(draftPunishment.getReason()).thenReturn("#hacking");
		when(draftPunishment.toBuilder()).thenReturn(newBuilder);
		when(newBuilder.reason("hello hackers")).thenReturn(newBuilder);
		when(newBuilder.build()).thenReturn(newPunishment);

		var event = fireEvent(draftPunishment);
		assertSame(newPunishment, event.getDraftSanction());
		verifyNoMoreInteractions(event.getSender());
	}

	@Test
	public void invalidShortcut(@Mock DraftPunishment draftPunishment) {
		when(config.shortcutIdentifier()).thenReturn("#");
		when(config.shortcuts()).thenReturn(Map.of(
				"hacking", "hello hackers",
				"spamming", "don't be a spammer, thank you"
		));
		when(draftPunishment.getReason()).thenReturn("#invalid");
		ComponentText invalidMessage = ComponentText.create(Component.text("Not a valid shortcut: %SHORTCUT_ARG%"));
		when(config.doesNotExist()).thenReturn(invalidMessage);

		var event = fireEvent(draftPunishment);
		assertTrue(event.isCancelled());
		verify(event.getSender()).sendMessage(
				ComponentText.create(Component.text("Not a valid shortcut: #invalid"))
		);
	}

	@AfterEach
	public void unregisterListener() {
		addon.shutdown();
	}

}
